package com.qring.print;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Context;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

final class PrinterManager {
    interface Callback { void done(boolean ok, String message); }
    interface SnapshotListener { void onSnapshot(PrinterSnapshot snapshot); }
    interface ProgressCallback { void onProgress(int finished,int total,String message); }
    interface RasterFactory { RasterEncoder.Raster create(int index) throws Exception; }

    private static final long QUERY_TIMEOUT_MS=1500;
    private static final long QUERY_SETTLE_MS=150;
    private static final long ACK_TIMEOUT_MS=120000;

    private static PrinterManager INSTANCE;
    static synchronized PrinterManager get(Context c) {
        if (INSTANCE==null) INSTANCE=new PrinterManager(c.getApplicationContext());
        return INSTANCE;
    }

    private final Context app;
    private final BluetoothAdapter adapter;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
    private final CopyOnWriteArrayList<SnapshotListener> listeners=new CopyOnWriteArrayList<>();
    private final PrinterSnapshot snapshot=new PrinterSnapshot();
    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;
    /** 独立接收缓冲：对应上游 sppRead + rxBuffer，避免 available() 轮询漏掉断线/ACK。 */
    private final Object rxLock=new Object();
    private final ArrayDeque<Byte> rxQueue=new ArrayDeque<>();
    private volatile boolean rxRunning;
    private volatile boolean transportAlive;
    private volatile IOException rxError;
    private volatile long rxGeneration;
    private Thread rxThread;
    private volatile boolean busy;
    private volatile boolean foreground=true;
    private volatile boolean cancelBatchRequested;
    private ScheduledFuture<?> pollFuture;

    private PrinterManager(Context c) {
        app=c;
        BluetoothManager bm=(BluetoothManager)c.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter=bm!=null ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();
    }

    boolean available(){ return adapter!=null; }
    boolean enabled(){ try{return adapter!=null && adapter.isEnabled();}catch(Throwable e){return false;} }
    boolean connected(){ BluetoothSocket s=socket; return transportAlive && s!=null && s.isConnected(); }
    boolean busy(){ return busy; }

    PrinterSnapshot snapshot(){ synchronized(snapshot){ return snapshot.copy(); } }
    void addListener(SnapshotListener l){ if(l!=null){listeners.addIfAbsent(l); l.onSnapshot(snapshot());} }
    void removeListener(SnapshotListener l){ listeners.remove(l); }

    private void notifySnapshot(){
        PrinterSnapshot p=snapshot();
        for(SnapshotListener l:listeners){ try{l.onSnapshot(p);}catch(Throwable ignored){} }
    }

    List<BluetoothDevice> bonded(Activity a) {
        ArrayList<BluetoothDevice> list=new ArrayList<>();
        try {
            if(adapter==null || !Compat.hasBluetoothPermissions(a)) return list;
            Set<BluetoothDevice> set=adapter.getBondedDevices();
            if(set!=null) list.addAll(set);
            Collections.sort(list, (x,y)->{
                boolean xm=matchesPrinterName(safeName(x)), ym=matchesPrinterName(safeName(y));
                if(xm!=ym) return xm?-1:1;
                return safeName(x).compareToIgnoreCase(safeName(y));
            });
        } catch(Throwable ignored){}
        return list;
    }

    static boolean matchesPrinterName(String n){
        if(n==null)return false;
        String s=n.toLowerCase(Locale.ROOT);
        return s.startsWith("qring") || s.contains("beeprt") || s.startsWith("by-") || s.startsWith("by_");
    }

    static String safeName(BluetoothDevice d) {
        try {
            String n=d.getName();
            return (n==null || n.trim().isEmpty()) ? "蓝牙设备" : n;
        } catch(Throwable e) { return "蓝牙设备"; }
    }

    void connect(Activity a, BluetoothDevice d, Callback cb) {
        io.execute(() -> {
            closeInternal(false);
            try {
                if(!Compat.hasBluetoothPermissions(a)) { post(a,cb,false,"缺少蓝牙权限"); return; }
                if(adapter!=null) adapter.cancelDiscovery();
                BluetoothSocket s=openSppSocket(d);
                socket=s; in=s.getInputStream(); out=s.getOutputStream(); transportAlive=true; startReceiver();
                synchronized(snapshot){
                    snapshot.connected=true; snapshot.deviceName=safeName(d);
                    try{snapshot.address=d.getAddress();}catch(Throwable ignored){}
                    snapshot.updatedAt=System.currentTimeMillis();
                }
                app.getSharedPreferences("printer",0).edit().putString("last_mac",d.getAddress()).apply();
                SavedPrinterStore.touch(app,d.getAddress(),safeName(d));
                refreshAllInternal();
                queryDeviceInfoInternal();
                if(foreground) startPolling();
                notifySnapshot();
                post(a,cb,true,"已连接 "+safeName(d));
            } catch(Throwable e) {
                closeInternal(false);
                post(a,cb,false,"连接失败："+friendly(e));
            }
        });
    }

    private BluetoothSocket openSppSocket(BluetoothDevice d) throws Exception {
        UUID spp=UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        Throwable first=null;
        try{BluetoothSocket s=d.createRfcommSocketToServiceRecord(spp);s.connect();return s;}catch(Throwable e){first=e;}
        try{BluetoothSocket s=d.createInsecureRfcommSocketToServiceRecord(spp);s.connect();return s;}catch(Throwable ignored){}
        // Some older BY/Qring firmwares only expose RFCOMM channel 1 correctly. Reflection is a last-resort compatibility path.
        try{java.lang.reflect.Method m=d.getClass().getMethod("createRfcommSocket",int.class);BluetoothSocket s=(BluetoothSocket)m.invoke(d,1);s.connect();return s;}catch(Throwable ignored){}
        if(first instanceof Exception)throw (Exception)first;
        throw new IOException("无法建立 SPP RFCOMM 连接");
    }

    void reconnectLast(Activity a, Callback cb) {
        String mac=app.getSharedPreferences("printer",0).getString("last_mac",null);
        if(mac==null || adapter==null){ post(a,cb,false,"没有上次连接的设备"); return; }
        try { connect(a,adapter.getRemoteDevice(mac),cb); }
        catch(Throwable e){ post(a,cb,false,"无法恢复设备："+friendly(e)); }
    }

    void disconnect(){ io.execute(() -> closeInternal(true)); }
    void requestCancelBatch(){ cancelBatchRequested=true; }

    void setForeground(boolean value){
        foreground=value;
        if(value && connected() && !busy) startPolling();
        else if(!value) stopPolling();
    }

    void setShutdownTime(Activity a,int seconds,Callback cb){
        io.execute(() -> {
            if(!connected()){post(a,cb,false,"打印机未连接");return;}
            if(busy){post(a,cb,false,"打印中，暂不能修改自动关机时间");return;}
            try{write(QringProtocol.shutdownTime(seconds));post(a,cb,true,"自动关机时间已下发");}
            catch(Throwable e){post(a,cb,false,"设置失败："+friendly(e));}
        });
    }

    void refresh(Activity a, Callback cb){
        io.execute(() -> {
            if(!connected()){post(a,cb,false,"打印机未连接");return;}
            try{refreshAllInternal(); notifySnapshot(); post(a,cb,true,"状态已刷新");}
            catch(Throwable e){post(a,cb,false,"状态读取失败："+friendly(e));}
        });
    }

    void print(Activity a, BitmapHolder holder, Callback cb) {
        if(holder==null||holder.raster==null){post(a,cb,false,"没有打印内容");return;}
        int copies=Math.max(1,Math.min(20,app.getSharedPreferences("settings",0).getInt("copies",1)));
        Ui.showPrintPreview(a,holder.raster,copies,true,selectedCopies->{
            int n=Math.max(1,Math.min(20,selectedCopies));
            app.getSharedPreferences("settings",0).edit().putInt("copies",n).apply();
            ArrayList<RasterEncoder.Raster> jobs=new ArrayList<>();
            for(int i=0;i<n;i++)jobs.add(holder.raster);
            printRastersNow(a,jobs,null,cb);
        },()->post(a,cb,false,"已取消打印"));
    }

    void printRasters(Activity a, List<RasterEncoder.Raster> rasters, ProgressCallback progress, Callback cb){
        if(rasters==null||rasters.isEmpty()){post(a,cb,false,"没有打印内容");return;}
        Ui.showPrintPreview(a,rasters.get(0),rasters.size(),()->printRastersNow(a,rasters,progress,cb),()->post(a,cb,false,"已取消打印"));
    }

    private void printRastersNow(Activity a, List<RasterEncoder.Raster> rasters, ProgressCallback progress, Callback cb){
        cancelBatchRequested=false;
        io.execute(() -> {
            if(busy){ post(a,cb,false,"已有打印任务"); return; }
            if(!connected()){ post(a,cb,false,"打印机未连接"); return; }
            if(rasters==null || rasters.isEmpty()){post(a,cb,false,"没有打印内容");return;}
            busy=true; stopPolling();
            try {
                String preflight=preflightInternal();
                if(preflight!=null){post(a,cb,false,preflight);return;}
                for(int i=0;i<rasters.size();i++){
                    if(cancelBatchRequested){post(a,cb,false,"批量打印已暂停");return;}
                    PrintOutcome r=printOneInternal(rasters.get(i));
                    if(progress!=null){int fin=r.ok?i+1:i; postProgress(a,progress,fin,rasters.size(),r.message);}
                    if(!r.ok){post(a,cb,false,"第 "+(i+1)+" 个任务失败："+r.message);return;}
                }
                post(a,cb,true,rasters.size()==1?"打印完成":"批量打印完成，共 "+rasters.size()+" 个任务");
            } catch(Throwable e) {
                closeInternal(false);
                post(a,cb,false,"打印失败："+friendly(e));
            } finally {
                busy=false;
                if(connected()){
                    try{refreshAllInternal();}catch(Throwable ignored){}
                    if(foreground) startPolling(); notifySnapshot();
                }
            }
        });
    }


    void printGenerated(Activity a,int total,RasterFactory factory,ProgressCallback progress,Callback cb){
        if(total<=0||factory==null){post(a,cb,false,"没有打印内容");return;}
        new Thread(()->{
            try{
                RasterEncoder.Raster first=factory.create(0);
                if(first==null){post(a,cb,false,"首条预览生成失败");return;}
                if(a==null){post(a,cb,false,"页面已关闭");return;}
                a.runOnUiThread(()->{
                    if(a.isFinishing()||a.isDestroyed()){post(a,cb,false,"页面已关闭");return;}
                    Ui.showPrintPreview(a,first,total,()->printGeneratedNow(a,total,index->index==0?first:factory.create(index),progress,cb),()->post(a,cb,false,"已取消打印"));
                });
            }catch(OutOfMemoryError e){post(a,cb,false,"生成打印预览时内存不足");}
            catch(Throwable e){post(a,cb,false,"生成打印预览失败："+friendly(e));}
        },"QrintPreview").start();
    }

    private void printGeneratedNow(Activity a,int total,RasterFactory factory,ProgressCallback progress,Callback cb){
        cancelBatchRequested=false;
        io.execute(() -> {
            if(busy){post(a,cb,false,"已有打印任务");return;}
            if(!connected()){post(a,cb,false,"打印机未连接");return;}
            if(total<=0||factory==null){post(a,cb,false,"没有打印内容");return;}
            busy=true; stopPolling();
            try{
                String preflight=preflightInternal();
                if(preflight!=null){post(a,cb,false,preflight);return;}
                for(int i=0;i<total;i++){
                    if(cancelBatchRequested){post(a,cb,false,"批量打印已暂停");return;}
                    RasterEncoder.Raster raster=factory.create(i);
                    if(raster==null){post(a,cb,false,"第 "+(i+1)+" 个任务生成失败");return;}
                    PrintOutcome r=printOneInternal(raster);
                    if(progress!=null){
                        final int done=r.ok?i+1:i;
                        final String message=r.message;
                        postProgress(a,progress,done,total,message);
                    }
                    if(!r.ok){post(a,cb,false,"第 "+(i+1)+" 个任务失败："+r.message);return;}
                }
                post(a,cb,true,total==1?"打印完成":"批量打印完成，共 "+total+" 个任务");
            }catch(OutOfMemoryError e){
                post(a,cb,false,"生成打印数据时内存不足，请缩小批次或页面");
            }catch(Throwable e){
                closeInternal(false);
                post(a,cb,false,"打印失败："+friendly(e));
            }finally{
                busy=false; cancelBatchRequested=false;
                if(connected()){
                    try{refreshAllInternal();}catch(Throwable ignored){}
                    if(foreground)startPolling(); notifySnapshot();
                }
            }
        });
    }

    private String preflightInternal() throws Exception{
        QringProtocol.Status s=queryStatusInternal();
        if(s==null)return null;
        synchronized(snapshot){snapshot.status=s;snapshot.updatedAt=System.currentTimeMillis();}
        return s.faultMessage();
    }

    RasterEncoder.Raster previewRaster(RasterEncoder.Raster raster){return applyPaperSettings(raster);}

    private RasterEncoder.Raster applyPaperSettings(RasterEncoder.Raster raster){
        if(raster==null)return null;
        android.content.SharedPreferences sp=app.getSharedPreferences("settings",0);

        // 机器只可靠报告“有纸/缺纸”，不会报告纸张/标签尺寸。这里所有尺寸都来自用户校准，
        // 然后统一换算为 203DPI 的真实点数。最终仍只发送 384 dots/行。
        float paperWidth=PrinterProfile.clampPaperWidthMm(sp.getInt("paper_width_tenths_mm",570)/10f);
        int paperDots=PrinterProfile.usableDotsForPaper(paperWidth);
        int widthMode=Math.max(0,Math.min(2,sp.getInt("content_width_mode",0))); // 0自动 1铺满纸宽 2自定义
        float customMm=Math.max(1f,sp.getInt("content_width_tenths_mm",Math.round(Math.min(paperWidth,PrinterProfile.printableWidthMm())*10f))/10f);
        int customDots=Math.max(1,Math.min(paperDots,PrinterProfile.mmToDots(customMm)));
        int paperAlign=Math.max(0,Math.min(2,sp.getInt("paper_alignment",0)));
        int contentAlign=Math.max(0,Math.min(2,sp.getInt("content_alignment",0)));
        int verticalAlign=Math.max(0,Math.min(2,sp.getInt("content_vertical_alignment",0)));
        int scale=Math.max(25,Math.min(200,sp.getInt("content_scale_percent",100)));
        boolean trimSides=sp.getBoolean("trim_side_blank",true);
        boolean label="label".equals(sp.getString("media_mode","continuous"));
        boolean trimBottom=!label && sp.getBoolean("auto_trim_length",true);
        int oldMm=Math.max(0,Math.min(1000,sp.getInt("paper_height_mm",0)));
        int labelTenths=Math.max(0,Math.min(10000,sp.getInt("label_length_tenths_mm",oldMm*10)));
        int fixedHeight=label && labelTenths>0?Math.min(8000,PrinterProfile.mmToDots(labelTenths/10f)):0;

        raster=RasterEncoder.layoutToMedia(raster,paperDots,widthMode,customDots,paperAlign,contentAlign,
                verticalAlign,scale,trimSides,trimBottom,fixedHeight);
        if(sp.getInt("print_direction",0)==180)raster=RasterEncoder.rotate180(raster);
        int yOffset=Math.max(-1000,Math.min(1000,sp.getInt("y_offset",0)));
        raster=RasterEncoder.shiftY(raster,yOffset);
        int xOffset=Math.max(-96,Math.min(96,sp.getInt("x_offset",0)));
        return RasterEncoder.shiftX(raster,xOffset);
    }

    private PrintOutcome printOneInternal(RasterEncoder.Raster raster) throws Exception {
        clearInput();
        raster=applyPaperSettings(raster);
        int density=app.getSharedPreferences("settings",0).getInt("density",1);
        int feedBefore=Math.max(0,Math.min(2000,app.getSharedPreferences("settings",0).getInt("feed_before",10)));
        int feedAfter=Math.max(0,Math.min(4000,app.getSharedPreferences("settings",0).getInt("feed_after",100)));
        write(QringProtocol.CMD_ENABLE);
        write(QringProtocol.CMD_ENABLE2);
        write(QringProtocol.thickness(density));
        write(QringProtocol.CMD_WAKEUP);
        for(byte[] f:QringProtocol.feed(feedBefore)) write(f);
        write(QringProtocol.rasterHeader(raster.height));
        writeChunked(raster.data);
        for(byte[] f:QringProtocol.feed(feedAfter)) write(f);
        write(QringProtocol.CMD_STOP);
        return waitAck(ACK_TIMEOUT_MS);
    }

    static final class BitmapHolder {
        final RasterEncoder.Raster raster;
        BitmapHolder(RasterEncoder.Raster r){ raster=r; }
    }
    private static final class PrintOutcome{
        final boolean ok; final String message; PrintOutcome(boolean o,String m){ok=o;message=m;}
    }

    private void startPolling(){
        if(!foreground || busy || !connected())return;
        stopPolling();
        pollFuture=poller.scheduleAtFixedRate(() -> {
            if(!connected() || busy)return;
            io.execute(() -> {
                if(!connected() || busy)return;
                try{refreshAllInternal(); notifySnapshot();}catch(Throwable e){if(e instanceof IOException)closeInternal(true);}
            });
        },10,10,TimeUnit.SECONDS);
    }
    private void stopPolling(){ ScheduledFuture<?> f=pollFuture; pollFuture=null; if(f!=null)f.cancel(false); }

    private void refreshAllInternal() throws Exception{
        if(!connected() || busy)return;
        QringProtocol.Status s=queryStatusInternal();
        Integer battery=queryBatteryInternal();
        synchronized(snapshot){
            if(s!=null)snapshot.status=s;
            if(battery!=null)snapshot.battery=battery;
            snapshot.connected=connected(); snapshot.updatedAt=System.currentTimeMillis();
        }
    }

    private QringProtocol.Status queryStatusInternal() throws Exception{
        byte[] r=query(QringProtocol.CMD_STATUS,1);
        return r.length<1?null:QringProtocol.parseStatus(r[0]&0xff);
    }
    private Integer queryBatteryInternal() throws Exception{
        byte[] r=query(QringProtocol.CMD_BATTERY,2);
        return r.length<2?null:(r[1]&0xff);
    }
    private void queryDeviceInfoInternal(){
        try{
            String model=queryString(QringProtocol.CMD_MODEL);
            String fw=queryString(QringProtocol.CMD_FW_VERSION);
            String sn=queryString(QringProtocol.CMD_SN);
            String bt=queryString(QringProtocol.CMD_BT_NAME);
            synchronized(snapshot){snapshot.model=model;snapshot.firmware=fw;snapshot.serialNumber=sn;snapshot.reportedBluetoothName=bt;snapshot.updatedAt=System.currentTimeMillis();}
        }catch(Throwable ignored){}
    }

    private String queryString(byte[] cmd) throws Exception{
        byte[] r=queryUpTo(cmd,64);
        StringBuilder b=new StringBuilder();
        for(byte x:r){int v=x&0xff;if(v>=0x20 && v<0x7f)b.append((char)v);}
        return b.toString().trim();
    }

    private byte[] query(byte[] cmd,int wanted) throws Exception{
        clearInput(); write(cmd); Thread.sleep(QUERY_SETTLE_MS);
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        long end=System.currentTimeMillis()+QUERY_TIMEOUT_MS;
        while(System.currentTimeMillis()<end && b.size()<wanted){
            int v=takeRx(Math.min(50L,Math.max(1L,end-System.currentTimeMillis())));
            if(v>=0)b.write(v);
        }
        return b.toByteArray();
    }

    private byte[] queryUpTo(byte[] cmd,int max) throws Exception{
        clearInput(); write(cmd); Thread.sleep(QUERY_SETTLE_MS);
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        long end=System.currentTimeMillis()+QUERY_TIMEOUT_MS;
        long quiet=Long.MAX_VALUE;
        while(System.currentTimeMillis()<end && b.size()<max){
            long now=System.currentTimeMillis();
            if(b.size()>0 && now>=quiet)break;
            int v=takeRx(Math.min(50L,Math.max(1L,end-now)));
            if(v>=0){b.write(v);quiet=System.currentTimeMillis()+100;}
        }
        return b.toByteArray();
    }

    /** 清空逻辑接收缓冲。底层 InputStream 只允许接收线程读取。 */
    private void clearInput(){
        synchronized(rxLock){rxQueue.clear();}
    }

    /**
     * Android RFCOMM 使用阻塞 read 接收。部分系统/蓝牙栈在 socket 被关闭或断开时 read() 会返回 -1，
     * 这里显式转为 EOF 并唤醒所有查询/ACK 等待者，避免表现成 120 秒假超时。
     */
    private void startReceiver(){
        stopReceiver(false);
        final long generation=++rxGeneration;
        rxRunning=true; rxError=null; transportAlive=true;
        synchronized(rxLock){rxQueue.clear();}
        final InputStream source=in;
        rxThread=new Thread(()->{
            byte[] buf=new byte[256];
            try{
                while(rxRunning && rxGeneration==generation){
                    int n=source.read(buf);
                    if(n<0)throw new EOFException("蓝牙连接已断开");
                    if(n==0)continue;
                    synchronized(rxLock){
                        if(rxGeneration!=generation)break;
                        for(int i=0;i<n;i++){
                            rxQueue.addLast(buf[i]);
                            while(rxQueue.size()>4096)rxQueue.removeFirst();
                        }
                        rxLock.notifyAll();
                    }
                }
            }catch(IOException e){
                if(rxRunning && rxGeneration==generation){rxError=e;transportAlive=false;markDisconnectedFromReceiver();}
            }catch(Throwable e){
                if(rxRunning && rxGeneration==generation){rxError=new IOException("蓝牙接收失败",e);transportAlive=false;markDisconnectedFromReceiver();}
            }finally{
                if(rxGeneration==generation)rxRunning=false;
                synchronized(rxLock){rxLock.notifyAll();}
            }
        },"QrintBtRx-"+generation);
        rxThread.setDaemon(true);
        rxThread.start();
    }

    private void markDisconnectedFromReceiver(){
        synchronized(snapshot){
            snapshot.connected=false; snapshot.status=null; snapshot.battery=null; snapshot.updatedAt=System.currentTimeMillis();
        }
        notifySnapshot();
        synchronized(rxLock){rxLock.notifyAll();}
    }

    private void stopReceiver(boolean clearError){
        rxRunning=false; transportAlive=false; rxGeneration++;
        synchronized(rxLock){rxLock.notifyAll(); if(clearError){rxError=null;rxQueue.clear();}}
    }

    /** 返回 0..255；超时返回 -1；连接异常直接抛出。 */
    private int takeRx(long waitMs) throws IOException,InterruptedException{
        long end=System.currentTimeMillis()+Math.max(1L,waitMs);
        synchronized(rxLock){
            while(rxQueue.isEmpty()){
                IOException e=rxError; if(e!=null)throw e;
                if(!transportAlive)throw new EOFException("蓝牙连接已断开");
                long remain=end-System.currentTimeMillis(); if(remain<=0)return -1;
                rxLock.wait(remain);
            }
            return rxQueue.removeFirst()&0xff;
        }
    }

    private void write(byte[] data) throws Exception {
        if(out==null) throw new IOException("输出流不可用");
        out.write(data); out.flush(); Thread.sleep(QringProtocol.CHUNK_DELAY_MS);
    }
    private void writeChunked(byte[] data) throws Exception {
        if(out==null) throw new IOException("输出流不可用");
        int off=0;
        while(off<data.length){
            int n=Math.min(QringProtocol.CHUNK_SIZE,data.length-off);
            out.write(data,off,n); out.flush(); off+=n; Thread.sleep(QringProtocol.CHUNK_DELAY_MS);
        }
    }

    private PrintOutcome waitAck(long timeout) throws Exception {
        long end=System.currentTimeMillis()+timeout;
        boolean previousWasFaultHead=false;
        while(System.currentTimeMillis()<end){
            final int v;
            try{v=takeRx(Math.min(100L,Math.max(1L,end-System.currentTimeMillis())));}
            catch(EOFException e){return new PrintOutcome(false,"连接已断开");}
            if(v<0)continue;
            if(v==QringProtocol.ACK_PRINT_DONE){clearInput();return new PrintOutcome(true,"打印完成");}
            if(previousWasFaultHead && v>=1 && v<=4){clearInput();return new PrintOutcome(false,QringProtocol.faultLabel(v));}
            previousWasFaultHead=(v==QringProtocol.FAULT_FRAME_HEAD);
        }
        return new PrintOutcome(false,"等待打印完成超时");
    }

    private void closeInternal(boolean notify){
        stopPolling(); stopReceiver(false);
        try { if(socket!=null) socket.close(); } catch(Throwable ignored){}
        try { if(in!=null) in.close(); } catch(Throwable ignored){}
        try { if(out!=null) out.close(); } catch(Throwable ignored){}
        in=null; out=null; socket=null;
        synchronized(rxLock){rxQueue.clear();rxError=null;rxLock.notifyAll();}
        synchronized(snapshot){snapshot.connected=false;snapshot.status=null;snapshot.battery=null;snapshot.updatedAt=System.currentTimeMillis();}
        if(notify)notifySnapshot();
    }

    private static void postProgress(Activity a,ProgressCallback cb,int done,int total,String msg){
        if(a==null||cb==null)return;
        a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())cb.onProgress(done,total,msg);});
    }
    private static void post(Activity a, Callback cb, boolean ok, String msg){
        if(a==null || cb==null)return;
        a.runOnUiThread(() -> { if(!a.isFinishing()&&!a.isDestroyed())cb.done(ok,msg); });
    }
    private static String friendly(Throwable e){ String m=e.getMessage(); return (m==null||m.trim().isEmpty())?e.getClass().getSimpleName():m; }
}
