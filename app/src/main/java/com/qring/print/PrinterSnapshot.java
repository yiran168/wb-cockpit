package com.qring.print;

final class PrinterSnapshot {
    boolean connected;
    String deviceName="";
    String address="";
    String model="";
    String firmware="";
    String serialNumber="";
    String reportedBluetoothName="";
    Integer battery=null;
    QringProtocol.Status status=null;
    long updatedAt=0L;

    PrinterSnapshot copy(){
        PrinterSnapshot p=new PrinterSnapshot();
        p.connected=connected;p.deviceName=deviceName;p.address=address;p.model=model;p.firmware=firmware;
        p.serialNumber=serialNumber;p.reportedBluetoothName=reportedBluetoothName;
        p.battery=battery;p.status=status;p.updatedAt=updatedAt;return p;
    }

    String summary(){
        if(!connected)return "未连接";
        StringBuilder b=new StringBuilder(deviceName.isEmpty()?"已连接":deviceName);
        if(status!=null)b.append(" · ").append(status.label());
        if(battery!=null)b.append(" · ").append(battery).append("%");
        return b.toString();
    }
}
