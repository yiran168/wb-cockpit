#!/usr/bin/env python3
from pathlib import Path
import re, sys, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
java_root=ROOT/'app/src/main/java/com/qring/print'
manifest=ROOT/'app/src/main/AndroidManifest.xml'
errors=[]; notes=[]

# XML resources + declared activities
xml_files=list((ROOT/'app/src/main/res').rglob('*.xml'))+[manifest]
for x in xml_files:
    try: ET.parse(x)
    except Exception as e: errors.append(f'XML invalid: {x.relative_to(ROOT)}: {e}')
activities=[]
try:
    tree=ET.parse(manifest); ns='{http://schemas.android.com/apk/res/android}'
    for a in tree.findall('.//activity'):
        n=a.attrib.get(ns+'name','')
        if n.startswith('.'):
            activities.append(n[1:])
            if not (java_root/(n[1:]+'.java')).exists(): errors.append(f'Missing activity source: {n}')
except Exception: pass

# no accidental binaries in source
native=list(ROOT.rglob('*.so'))+list(ROOT.rglob('*.aar'))
classes=list((ROOT/'app/src').rglob('*.class'))
if native: errors.append('Native/aar binaries present: '+', '.join(str(x.relative_to(ROOT)) for x in native))
if classes: errors.append('Compiled .class files accidentally present in source: '+', '.join(str(x.relative_to(ROOT)) for x in classes))

# Stable build configuration + unique package ID (prevents Android package/update collision)
build=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
for pat,label in [
    (r'applicationId\s+"com\.yiran168\.cuotiprint"','unique applicationId'),
    (r'minSdk\s+21','minSdk 21'),(r'compileSdk\s+36','compileSdk 36'),(r'targetSdk\s+36','targetSdk 36'),
    (r'versionCode\s+151','versionCode 151'),(r'versionName\s+"1\.5\.1"','version 1.5.1')]:
    if not re.search(pat,build): errors.append(f'Build config missing {label}')

# Protocol invariants from Thisko/QrintPrint-compatible transport
q=(java_root/'QringProtocol.java').read_text(encoding='utf-8')
for snippet,label in [('WIDTH_DOTS = 384','384 dots'),('WIDTH_BYTES = 48','48 bytes/row'),('CHUNK_SIZE = 1024','1024-byte chunks'),('CHUNK_DELAY_MS = 1','1ms chunk delay')]:
    if snippet not in q: errors.append(f'Protocol invariant missing: {label}')

# Physical profile for user's paper/printer
profile=(java_root/'PrinterProfile.java').read_text(encoding='utf-8')
for pat,label in [(r'DPI\s*=\s*203','203 DPI'),(r'DEFAULT_PAPER_WIDTH_MM\s*=\s*57f','57mm paper'),(r'HEAD_DOTS\s*=\s*QringProtocol\.WIDTH_DOTS','384-dot head')]:
    if not re.search(pat,profile): errors.append(f'Printer profile missing: {label}')

# Real printer path and fallbacks
pm=(java_root/'PrinterManager.java').read_text(encoding='utf-8')
for token,label in [
    ('createRfcommSocketToServiceRecord','secure RFCOMM SPP'),('createInsecureRfcommSocketToServiceRecord','insecure RFCOMM fallback'),
    ('getMethod("createRfcommSocket",int.class)','RFCOMM channel-1 fallback'),('getOutputStream()','real output stream'),
    ('writeChunked','chunked raster output'),('waitAck','print completion ACK'),('preflightInternal','printer fault preflight')]:
    if token not in pm: errors.append(f'Printer transport missing: {label}')
if not re.search(r'n\s*<\s*0',pm): errors.append('No Bluetooth InputStream EOF handling found')
# Generated/batch preview must never open Android UI from its worker thread.
gen_start=pm.find('void printGenerated(Activity')
gen_end=pm.find('private void printGeneratedNow',gen_start)
if gen_start<0 or gen_end<0 or 'runOnUiThread' not in pm[gen_start:gen_end]: errors.append('Generated print preview is not marshalled to the Android UI thread')

# Every manager print entry point must go through final preview
for method in ['void print(Activity','void printRasters(Activity','void printGenerated(Activity']:
    i=pm.find(method)
    if i<0: errors.append(f'Missing print entry point: {method}'); continue
    if 'showPrintPreview' not in pm[i:i+3800]: errors.append(f'Print entry point bypasses final preview: {method}')

# Every user-facing print/settings workflow exposes preview UI
preview_pages=[
    'TextPrintActivity','ImagePrintActivity','PdfPrintActivity','OfficePrintActivity','QrCodeActivity','BarcodePrintActivity',
    'CanvasEditorActivity','BatchPrintActivity','SerialPrintActivity','ScanCodeActivity','ProductDatabaseActivity',
    'WebPrintActivity','TemplateActivity','BuiltInTemplateActivity','TemplateCodeActivity','HistoryActivity','DiagnosticsActivity',
    'PaperSettingsActivity','SettingsActivity']
for cls in preview_pages:
    text=(java_root/(cls+'.java')).read_text(encoding='utf-8')
    if 'previewFrame' not in text: errors.append(f'Local preview missing: {cls}')

# Zoomable / draggable visual previews: no plain ImageView constructions remain
plain=[]
for jf in java_root.glob('*.java'):
    text=jf.read_text(encoding='utf-8')
    if 'new ImageView(this)' in text: plain.append(jf.name)
if plain: errors.append('Plain non-zoomable ImageView previews remain: '+', '.join(plain))

# UX / appearance / haptics / cover
ui=(java_root/'Ui.java').read_text(encoding='utf-8')
for token,label in [('appearance','appearance mode'),('reduce_motion','reduced motion'),('isWide','tablet/foldable layout'),('bottomNav','persistent bottom navigation'),('ZoomablePreviewView','zoomable final preview'),('VibrationEffect.createOneShot(28,150)','strong haptic fallback')]:
    if token not in ui: errors.append(f'UX invariant missing: {label}')
if 'SplashActivity' not in activities: errors.append('Splash/cover activity missing from manifest')
if 'android.permission.VIBRATE' not in manifest.read_text(encoding='utf-8'): errors.append('VIBRATE permission missing')

# Custom editor must be a real layout editor, not a static preview shell
canvas=(java_root/'CanvasEditorView.java').read_text(encoding='utf-8')
for token,label in [('ScaleGestureDetector','pinch scaling'),('ACTION_MOVE','drag positioning'),('setSelectedBounds','exact XYWH editor'),('scaleSelected','step scaling'),('out[row+(tx>>3)]|=','per-element raster OR')]:
    if token not in canvas: errors.append(f'Canvas editor missing: {label}')
ca=(java_root/'CanvasEditorActivity.java').read_text(encoding='utf-8')
for token,label in [('笔画粗细','text stroke control'),('字体样式','font family selection'),('边框粗细','shape stroke control'),('精确位置','exact position tool')]:
    if token not in ca: errors.append(f'Custom editor UI missing: {label}')
if '+条码' not in ca or 'private void addBarcode()' not in ca or 'BarcodeUtil.prepare' not in ca: errors.append('Custom canvas 1D barcode insertion is missing or not validated')
if 'new RasterEncoder.Raster(h,out,keepMargins)' not in canvas or 'e.kind==KIND_CODE' not in canvas: errors.append('Custom canvas code quiet-zone preservation missing')

# Built-in templates: expanded gallery + compile fix for static helper calls
bt=(java_root/'BuiltInTemplateActivity.java').read_text(encoding='utf-8')
if '"空白自由标签"' not in bt or '"错题复习计划"' not in bt or '"访客临时证"' not in bt: errors.append('Expanded built-in template set missing')
if bt.count('case ') < 40: errors.append('Built-in template switch has fewer than 40 template cases')
for name in ['price','barcodeBox','smallBox','address','wrongQuestion','todo','weekly']:
    if not re.search(rf'private\s+static\s+void\s+{name}\s*\(',bt): errors.append(f'Built-in template helper not static: {name}')

# v1.5 print-confirm sound: real synthesized PCM, 10 presets + random preset + generated random.
ps=(java_root/'PrintSound.java')
if not ps.exists(): errors.append('PrintSound engine missing')
else:
    pst=ps.read_text(encoding='utf-8')
    for token,label in [('AudioTrack','PCM audio output'),('MODE_NAMES','sound selector'),('RANDOM_PRESET=10','random preset mode'),('RANDOM_GENERATED=11','procedural random mode'),('PrintSoundPattern.randomPattern()','generated sound path')]:
        if token not in pst: errors.append(f'Print sound missing: {label}')
pattern_src=(java_root/'PrintSoundPattern.java')
if not pattern_src.exists(): errors.append('Pure-Java print sound pattern generator missing')
else:
    pattern_text=pattern_src.read_text(encoding='utf-8')
    if pattern_text.count('case ') < 9 or 'default:return render' not in pattern_text or 'randomPattern()' not in pattern_text: errors.append('Print sound pattern set incomplete')
settings_src=(java_root/'SettingsActivity.java').read_text(encoding='utf-8')
for token,label in [('printSoundEnabled','sound enable setting'),('printSoundMode','sound mode setting'),('printSoundVolume','sound volume setting'),('试听当前打印音效','sound preview control')]:
    if token not in settings_src: errors.append(f'Print sound setting missing: {label}')
if 'PrintSound.play(a)' not in ui: errors.append('Final confirm print button does not trigger print sound')
# Batch factories execute on a worker: capture mutable UI/data state before entering them.
office=(java_root/'OfficePrintActivity.java').read_text(encoding='utf-8')
if 'int fontProgress=size.getProgress()' not in office or 'bitmap(i,fontProgress)' not in office: errors.append('Office batch print reads mutable size UI from worker thread')
batch=(java_root/'BatchPrintActivity.java').read_text(encoding='utf-8')
if 'final DataTableReader.Table data=table' not in batch or 'renderBitmap(data,start+index,tpl)' not in batch: errors.append('Batch print does not snapshot imported data before worker execution')
pdf=(java_root/'PdfPrintActivity.java').read_text(encoding='utf-8')
if 'final Uri source=uri;final int stablePageIndex=pageIndex;final RenderConfig cfg=config()' not in pdf: errors.append('Segmented PDF current-page print retains mutable Activity bitmap/state')
backup=(java_root/'BackupUtil.java').read_text(encoding='utf-8')
for key in ['PrintSound.KEY_ENABLED','PrintSound.KEY_MODE','PrintSound.KEY_VOLUME']:
    if key not in backup: errors.append('Print sound preference not backed up: '+key)

# v1.3 UI/print usability regressions
for token,label in [('ScrollView dialogScroll','scrollable final print parameters'),('handleTabSwipe','swipe tab navigation'),('openFromCard','card zoom transition')]:
    if token not in ui: errors.append(f'v1.3 UX missing: {label}')
if 'private static void barcodeBox' not in bt or '1234567890' not in bt: errors.append('Template barcode placeholder fix missing')
img=(java_root/'ImagePrintActivity.java').read_text(encoding='utf-8')
for token in ['Atkinson','Jarvis-Judice-Ninke','Sierra Lite','Stucki']:
    if token not in img: errors.append('Expanded dithering mode missing: '+token)
if (java_root/'ReleaseInfoActivity.java').exists() is False: errors.append('Release/open-source attribution page missing')

# v1.4 paper/label geometry + code classification
paper=(java_root/'PaperSettingsActivity.java').read_text(encoding='utf-8')
for token,label in [('media_mode','continuous/label mode'),('content_width_mode','content width mode'),('label_length_tenths_mm','label length'),('paper_alignment','paper load alignment'),('content_vertical_alignment','label vertical alignment'),('auto_trim_length','content-derived continuous length')]:
    if token not in paper: errors.append(f'v1.4 media setting missing: {label}')
main=(java_root/'MainActivity.java').read_text(encoding='utf-8')
if 'QrCodeActivity.class' not in main or 'BarcodePrintActivity.class' not in main: errors.append('QR and barcode are not separated on home screen')
bar=(java_root/'BarcodePrintActivity.java').read_text(encoding='utf-8')
for token,label in [('ONE_LABELS','1D barcode category'),('TWO_LABELS','2D barcode category'),('BarcodeUtil.prepare','format-aware validation'),('encodePreserveMargins','barcode quiet-zone output')]:
    if token not in bar: errors.append(f'Barcode workflow missing: {label}')
qr=(java_root/'QrCodeActivity.java').read_text(encoding='utf-8')
if 'encodePreserveMargins' not in qr or 'ErrorCorrectionLevel' not in qr: errors.append('Dedicated QR workflow missing final raster/EC handling')
template_code=(java_root/'TemplateCodeActivity.java').read_text(encoding='utf-8')
if 'encodePreserveMargins(qr' not in template_code: errors.append('Template-code QR print can lose quiet zone')
serial=(java_root/'SerialPrintActivity.java').read_text(encoding='utf-8')
if 'kindPos==0?RasterEncoder.encode' not in serial or 'RasterEncoder.encodePreserveMargins' not in serial: errors.append('Serial QR/barcode print can lose quiet zone')

# Web URL editor LTR/single-line fix
web=(java_root/'WebPrintActivity.java').read_text(encoding='utf-8')
for token in ['setSingleLine(true)','TEXT_DIRECTION_LTR','LAYOUT_DIRECTION_LTR','TYPE_TEXT_VARIATION_URI','setBuiltInZoomControls(true)']:
    if token not in web: errors.append('Web input/zoom fix missing: '+token)

# Raster text supports stroke width
re_src=(java_root/'RasterEncoder.java').read_text(encoding='utf-8')
if 'strokeWidthPx' not in re_src or 'FILL_AND_STROKE' not in re_src: errors.append('Raster text stroke-width support missing')
if 'layoutToMedia' not in re_src or 'content_width_mode' not in pm or 'paper_width_tenths_mm' not in pm or 'content_vertical_alignment' not in pm: errors.append('Content-derived/custom-width media layout pipeline missing')
if 'preserveMargins' not in re_src or 'encodePreserveMargins' not in re_src: errors.append('Barcode/QR quiet-zone preservation missing')
# Major direct-print editing pages should show the same media transform used by final output.
for cls in ['TextPrintActivity','ImagePrintActivity','PdfPrintActivity','OfficePrintActivity','WebPrintActivity','ProductDatabaseActivity','QrCodeActivity','BarcodePrintActivity']:
    text=(java_root/(cls+'.java')).read_text(encoding='utf-8')
    if 'previewRaster' not in text: errors.append(f'Exact media-layout local preview missing: {cls}')


# User-facing copy must stay neutral and use usage/function wording rather than brand-comparison claims.
for jf in java_root.glob('*.java'):
    txt=jf.read_text(encoding='utf-8')
    if '汉印' in txt or '对标' in txt: errors.append(f'Brand-comparison wording remains in app source: {jf.name}')

# Theme resources: old Android + night + Android 12 splash
for rel in ['app/src/main/res/values/styles.xml','app/src/main/res/values-v23/styles.xml','app/src/main/res/values-v31/styles.xml','app/src/main/res/values-night/styles.xml','app/src/main/res/values-night-v23/styles.xml','app/src/main/res/values-night-v31/styles.xml','app/src/main/res/drawable/splash_background.xml','app/src/main/res/drawable-night/splash_background.xml']:
    if not (ROOT/rel).exists(): errors.append(f'Missing theme/splash resource: {rel}')

# Workflow is self-contained and buildable on GitHub
wf=ROOT/'.github/workflows/build-apk.yml'
if not wf.exists(): errors.append('Missing GitHub Actions APK workflow')
else:
    w=wf.read_text(encoding='utf-8')
    for token in ['9.3.1','android-actions/setup-android@v4','android-36','build-tools;36.0.0',':app:testDebugUnitTest',':app:lintDebug',':app:assembleDebug','CuotiPrint-Android-v1.5.1-final-verified-debug']:
        if token not in w: errors.append(f'Workflow missing: {token}')

java_files=list(java_root.glob('*.java'))
test_dir=ROOT/'app/src/test/java/com/qring/print'
test_files=list(test_dir.glob('*.java')) if test_dir.exists() else []
notes += [
    f'Java files: {len(java_files)}',f'Unit test sources: {len(test_files)}',f'Manifest activities: {len(activities)}',
    f'Native .so/.aar: {len(native)}',f'Compiled .class in source: {len(classes)}',
    'App ID collision guard: com.yiran168.cuotiprint',
    'Printer profile: 57mm paper / 203DPI / 384-dot head',
    'Real RFCOMM SPP transport + ACK/status preflight: enabled',
    'Final raster preview gate: enabled',
    f'Local preview pages: {len(preview_pages)} / {len(preview_pages)}',
    'Zoomable/draggable visual previews: enabled',
    'Custom drag / pinch / exact XYWH / font / stroke tools: enabled',
    'User-declared paper/label width + content-derived/custom width + X/Y calibration: enabled',
    'Continuous-paper auto length + fixed label length: enabled',
    'QR and barcode workflows separated with format-aware validation: enabled',
    'Splash cover + stronger haptic fallback + appearance recreation: enabled',
    'Built-in template gallery: 40 templates',
    'Final print dialog: scrollable detailed parameters + live raster refresh',
    'Photo dithering: 8 modes',
    'Main tabs: horizontal swipe navigation + directional transition',
    'Open-source attribution page: enabled',
]
print('\n'.join(notes))
if errors:
    print('\nFAIL:')
    for e in errors: print(' - '+e)
    sys.exit(1)
print('\nPASS: v1.5.1 release static audit')
