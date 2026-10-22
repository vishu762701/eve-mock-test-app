package com.eve.app.designsystem

/** Pure progress reducer: false-positive checkmarks are prohibited. */
internal data class UploadStepState(
    val progress: Int,
    val uploading: Boolean,
    val uploaded: Boolean,
    val failed: Boolean,
    val message: String
) {
    companion object {
        fun from(
            examReady: Boolean,
            pdfReady: Boolean,
            uploading: Boolean,
            uploaded: Boolean,
            failed: Boolean
        ): UploadStepState {
            val validPdf = examReady && pdfReady
            val confirmed = validPdf && uploaded
            val working = validPdf && uploading && !confirmed
            val errored = validPdf && failed && !confirmed && !working
            val progress = when {
                confirmed -> 3
                validPdf -> 2
                examReady -> 1
                else -> 0
            }
            val message = when {
                confirmed -> "All steps complete. Syllabus upload confirmed."
                errored -> "Step 3 of 3: Upload failed. Retry upload."
                working -> "Step 3 of 3: Upload in progress."
                progress == 2 -> "Step 3 of 3: Ready to upload PDF."
                progress == 1 -> "Step 2 of 3: Select a valid PDF."
                else -> "Step 1 of 3: Choose an exam."
            }
            return UploadStepState(progress, working, confirmed, errored, message)
        }
    }
}
