package com.qring.print;

/**
 * Physical profile for the 57/58mm-class Qring/BeePrt mechanism used by QrintPrint.
 *
 * The printer can report paper-present / paper-out, but it does not report the paper or label
 * dimensions.  Width and label length are therefore user-calibrated layout parameters.  The
 * thermal head itself is always 384 dots wide, so every preview and every print job is finally
 * rendered into the exact same 384-dot raster coordinate system.
 */
final class PrinterProfile {
    static final int DPI = 203;
    static final float DEFAULT_PAPER_WIDTH_MM = 57f;
    static final float MIN_PAPER_WIDTH_MM = 10f;
    static final float MAX_PAPER_WIDTH_MM = 57f;
    static final int HEAD_DOTS = QringProtocol.WIDTH_DOTS;
    static final int HEAD_BYTES = QringProtocol.WIDTH_BYTES;

    private PrinterProfile() {}

    static float printableWidthMm() { return dotsToMm(HEAD_DOTS); }
    static int mmToDots(float mm) { return Math.max(0, Math.round(mm * DPI / 25.4f)); }
    static float dotsToMm(int dots) { return dots * 25.4f / DPI; }

    /** User-entered media width, not a sensor result. */
    static float clampPaperWidthMm(float mm) {
        return Math.max(MIN_PAPER_WIDTH_MM, Math.min(MAX_PAPER_WIDTH_MM, mm));
    }

    /**
     * Number of head dots physically usable by a centered/left/right label of the requested width.
     * 57mm paper still maps to 384 dots because the head is narrower than the paper.
     */
    static int usableDotsForPaper(float paperWidthMm) {
        return Math.max(1, Math.min(HEAD_DOTS, mmToDots(clampPaperWidthMm(paperWidthMm))));
    }

    static float sideMarginMm(float paperWidthMm) {
        return Math.max(0f, (clampPaperWidthMm(paperWidthMm) - printableWidthMm()) / 2f);
    }
}
