# QR active-path reconciliation

Live base: e0963d7dcdfec04bcbe90f56dea26246b549e93b, refetched main with no
concurrent commit. Exact-SHA FAST 41403654857 SUCCESS, docs 41403654877 SUCCESS,
verbatim imports 41403654789 SUCCESS. Native navigation/publication skipped;
artifact count zero. Official Telegram pin remains
3de37c8cfb039452be7398909461da6288babf15. Whole QR acceptance stays OPEN.

Latest owner steering: preserve green QR, fix only proven critical functional
defects, record other gaps, continue Profile/Admin/Home/Exam/shared implementation.
The uncommitted corner-cache/analytic-dim experiment was removed from production
and tests. The committed overlay is restored byte-for-byte. No successful QR
implementation is deleted, replaced or replayed.

## Production caller audit

Inspected manifest routes, ProfileActivity, complete UsernameCardActivity entry/
picker/save/share paths, TelegramQrSceneView state/drawing/lifecycle, scanner
camera/gallery/result/permission/torch paths, picker/grid/native asset owners and
source-component references. This is source/host evidence, not physical acceptance.

| Implemented feature | Actual production path and ownership | Finding / remaining classification |
|---|---|---|
| Public identity/navigation | ProfileActivity.showPersonalQr -> ProfileQrPayload.encode -> UsernameCardActivity; exported manifest username VIEW route; scanner routes the same Activity | One personal viewer route. Username-only parser rejects foreign authority/query/fragment/encoded path. Device-only acceptance gap for external intent matrix. |
| Scene/card/caption/E emblem | UsernameCardActivity -> one TelegramQrSceneView -> QrCardLayout/QrCardCaption/QrCardShadow/QrCodeLayer -> TelegramQRCodeWriter | One scene and one source QR encoder. Independent temporary export layout is intentional isolation. Source-fidelity gap for whole original composition/reference parity. |
| Native backdrop/palettes/wave/ink/lighting | Scene -> QrBackdropScene -> QrBackdropNative/JNI; Backdrops/QrPalettes/QrPaletteInk/QrSceneShine are collaborators | Separate responsibilities, not competing scene renderers. Source-fidelity and device-only GPU/fallback gaps. |
| Reveal/close/card motion | Scene.bindSceneLayer -> QrSceneReveal; QrCardMotion owns card response | Different animation channels, not duplicate navigation/state owners. Device-only interruption/window matrix gap. |
| Pattern gestures/swap/appearance | Scene -> QrPatternField/QrPatternSwap/QrEmojiAppearance | Scene owns current/outgoing selections; finite outgoing transition is intentional. Source-fidelity/reference gap. |
| Scene glass and picker lift | Scene/root native sampling -> QrControlBackground/QrGlass/RimPainter/RimTune/QrPickerSheetLift/QrRender | Shared primitives/camera adapters are not duplicate screens. Device-only RenderNode/GPU acceptance gap. |
| Colors | UsernameCardActivity.showQrPicker(false) -> color grid -> scene.setPalette | One color picker; Material parent is source-fidelity gap. |
| Animated picker | showQrPicker(true) -> showAnimatedEmojiPicker -> QrPickerMorphDialog/Surface -> QrEmojiPickerGrid -> Java QrEmojiGrid/QrEmojiCell/EmojiPickOverlay | One active animated picker. Outer shape, grid reveal and cell cascade are different motion channels. Material parent/drag/island blur are source-fidelity gaps. |
| Six-item selection/restoration | Scene.selectedEmojis is authoritative; picker receives picks/toggle closures; Bundle restores keys/alts/sources | No second persistent selection owner. Legacy standard glyphs are explicitly static saved-data compatibility, never claimed animated parity. Device-only recreation/large-font matrix gap. |
| Native assets/playback | QrAnimationAssets -> NativeQrDrawable -> pinned tlottie; scene and recycled cells own distinct-sized frame instances | Distinct view/render resources, not redundant business state. HTTP/decompression/frame bounds and cancellation exist. Device-only scheduling/memory acceptance; EVE catalogue availability is external dependency. |
| Scanner/morph/frame | UsernameCardActivity scan action -> sole non-exported EveQrLiveScannerActivity -> QrScanMorphLayout/QrScanMorph + QrScannerFrameOverlay/QrScanFound | One scanner; three actual ZXing points never synthesize a fourth. Source-fidelity gap: original corner bitmap optimization. Device-only camera crop/orientation/timing evidence. |
| Camera/permission/Gallery | scanner -> CameraX PreviewView/shared ViewPort + session generation; ActivityResult permission/Settings/GetContent -> bounded worker decoding | One camera-session owner; Gallery uses same decoder/result handler. Device-only camera/permanent denial/Settings/rotation matrix. |
| Scanner controls/torch/blur | scanner -> QrScannerLayout/QrScannerControl/BackDrawable/QrScannerGlass/QrCameraBackdrop; QrTorchGate owns confirmed future state | One torch owner. Control view selection reflects confirmed state. Source-fidelity and device-only HWUI/physical torch gaps. |
| Save | saveToGallery -> renderShareImage -> QrGalleryWriter | One publisher, pending MediaStore publication/rollback; legacy permission path retained. Device-only actual Gallery/storage failure matrix gap. |
| Share | shareImage -> renderShareImage -> FileProvider content URI + ClipData/read grant | One export renderer, no chrome. Existing bitmap scan tests pass. Device-only receiver/chooser/interruption matrix gap. |
| Payment QR (separate feature) | PaymentCheckoutActivity -> util.QrCodeGenerator.generateBitmap | Active existing payment-specific generator, not an alternate personal QR path. No payment change authorized by source-fidelity cleanup. Its standalone QR conformance is unverified; investigate separately without claiming it valid or deleting it. |
| Test/compatibility helpers | ProfileQrPayload.matrix and TelegramQrMotion have test references, no production callers found by repository-wide search | Inactive in production; not duplicate active implementations. Retained to avoid unrelated cleanup. |

## Remaining issue classes

Critical functional defect: none newly reproduced in the personal QR production
paths during this reconciliation. This is not a claim that every device path is
bug-free. Existing permission/session/torch/deduplication fixes are preserved.

Source-fidelity gaps: full scene/parent composition, Material color/emoji modal
container and gestures, complete island/picker compositor, source cached corners,
whole typography/resource/theming/accessibility equivalence. Record and defer;
missing device/reference evidence does not justify more speculative QR code.

Device-only acceptance gaps: physical CameraX transformations, four-point timing,
real torch failures, permissions/Settings/Gallery returns, HWUI blur/fallback,
interrupted motion, rotation/process restoration, both navigation modes, Day/Night,
Hindi/RTL/font/display scale/TalkBack and 60/90/120Hz/memory/reference captures.

External/legal dependencies: real EVE-owned animated catalogue availability;
unavailable Telegram gifts/premium inventory/account services (not fabricated;
QR remains free); human per-file/font/combined-app distribution review and
EVE_GPL_RELEASE_APPROVED. FULL/native/release gates remain intact and unpassed.

Next implementation is outside QR: Profile avatar renderer resource lifecycle,
then other concrete Profile/Admin/Home/Exam/shared ledger requirements. The whole
mission remains NOT MET; hardware/legal limitations are not a global work blocker.
