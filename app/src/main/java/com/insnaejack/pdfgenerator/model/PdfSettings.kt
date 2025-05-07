package com.insnaejack.pdfgenerator.model

// All dimensions are in points (1/72 inch)
enum class PageSize(val width: Int, val height: Int, val displayName: String) {
    A4(595, 842, "A4 (210 x 297 mm)"),
    LETTER(612, 792, "Letter (8.5 x 11 in)"),
    LEGAL(612, 1008, "Legal (8.5 x 14 in)");
    // Add more common page sizes if needed

    override fun toString(): String = displayName
}

enum class PageOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape");

    override fun toString(): String = displayName
}

enum class ImageQuality(val compressionQuality: Int, val displayName: String) {
    LOW(50, "Low (Smaller File Size)"),       // Example: 50% JPEG quality
    MEDIUM(75, "Medium (Balanced)"),    // Example: 75% JPEG quality
    HIGH(95, "High (Larger File Size)");   // Example: 95% JPEG quality

    override fun toString(): String = displayName
}

data class PdfSettings(
    val pageSize: PageSize = PageSize.A4,
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val imageQuality: ImageQuality = ImageQuality.MEDIUM,
    val marginTop: Int = 36, // 0.5 inch
    val marginBottom: Int = 36,
    val marginLeft: Int = 36,
    val marginRight: Int = 36
) {
    val pageDisplayWidth: Int
        get() = if (orientation == PageOrientation.PORTRAIT) pageSize.width else pageSize.height

    val pageDisplayHeight: Int
        get() = if (orientation == PageOrientation.PORTRAIT) pageSize.height else pageSize.width

    val contentWidth: Int
        get() = pageDisplayWidth - marginLeft - marginRight

    val contentHeight: Int
        get() = pageDisplayHeight - marginTop - marginBottom
}