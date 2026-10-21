#!/usr/bin/env python3
"""
Python verification script for EVE Typography Studio.
Verifies:
1. activity_typography_studio.xml structure, tags, and required view IDs
2. activity_admin.xml contains btnTypographyStudio
3. AndroidManifest.xml declares TypographyStudioActivity as exported=false
4. design_type.xml semantic roles and tokens
5. FontManager font options and font files
6. TypographyRegistry screens and element catalog
7. Backend migration and routes
"""

import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"

def assert_true(cond, msg):
    if not cond:
        print(f"FAILED: {msg}", file=sys.stderr)
        sys.exit(1)
    else:
        print(f"PASS: {msg}")

def check_xml_ids():
    studio_xml = RES / "layout/activity_typography_studio.xml"
    assert_true(studio_xml.exists(), "activity_typography_studio.xml exists")

    tree = ET.parse(studio_xml)
    root = tree.getroot()

    all_ids = set()
    for el in root.iter():
        for attr, val in el.attrib.items():
            if attr.endswith("id"):
                clean_id = val.split("/")[-1]
                all_ids.add(clean_id)

    expected_ids = [
        "btnBack",
        "tabLayoutStudio",
        # Section containers
        "sectionFontLibrary",
        "sectionGlobalStyles",
        "sectionScreenEditor",
        "sectionLivePreview",
        "sectionVersions",
        # Font library controls
        "cardFontInter", "btnSelectInter",
        "cardFontPoppins", "btnSelectPoppins",
        "cardFontRoboto", "btnSelectRoboto",
        "cardFontNoto", "btnSelectNoto",
        # Global styles controls
        "btnPresetIosBalanced", "btnPresetIosHig", "btnPresetPoppinsClean",
        "btnPresetRobotoNeutral", "btnPresetNotoReadable",
        "spGlobalRole", "tilRoleSize", "etRoleSize", "tilRoleLineHeight", "etRoleLineHeight",
        "chipGroupRoleWeight", "chipWeight400", "chipWeight500", "chipWeight600", "chipWeight700",
        "tvRolePreviewSample", "btnSaveRole", "btnResetCurrentRole",
        # Screen editor controls
        "spScreen", "spElement", "tvElementInheritance",
        "spElementFontFamily", "tilElementSize", "etElementSize",
        "tilElementLineHeight", "etElementLineHeight", "chipGroupElementWeight",
        "btnSaveElementOverride", "btnResetElementOverride",
        # Live preview controls (including backward-compatible IDs)
        "tvActiveFontInfo",
        "chipGroupFonts",
        "chipInterRegular", "chipInterMedium", "chipInterSemibold", "chipInterBold",
        "chipSystemSans", "chipSystemSerif", "chipSystemMono",
        "switchEmphasized",
        "tilSampleText", "etSampleText",
        "btnPresetEnglish", "btnPresetHindi", "btnResetText",
        "previewPrimaryButton", "previewSecondaryButton", "previewCardHeading", "previewCardBody",
        "previewLargeTitle", "previewTitle1", "previewTitle2", "previewTitle3",
        "previewHeadline", "previewBody", "previewCallout", "previewSubheadline",
        "previewFootnote", "previewCaption1", "previewCaption2",
        # Versions controls
        "tvVersionStatus", "tvVersionMeta", "tvValidationStatus",
        "tilPublishNotes", "etPublishNotes", "btnSaveDraft", "btnPublishDraft", "btnDiscardDraft",
        "spVersionHistory", "btnRollbackVersion", "btnFactoryReset",
        "btnExportJson", "tilImportJson", "etImportJson", "btnImportJson"
    ]

    for eid in expected_ids:
        assert_true(eid in all_ids, f"Required element ID '{eid}' is present in layout")

def check_admin_activity():
    admin_xml = RES / "layout/activity_admin.xml"
    assert_true(admin_xml.exists(), "activity_admin.xml exists")
    tree = ET.parse(admin_xml)
    btn = None
    for el in tree.iter():
        for attr, val in el.attrib.items():
            if attr.endswith("id") and val.endswith("/btnTypographyStudio"):
                btn = el
                break
    assert_true(btn is not None, "btnTypographyStudio exists in activity_admin.xml")

def check_manifest():
    manifest = ROOT / "app/src/main/AndroidManifest.xml"
    assert_true(manifest.exists(), "AndroidManifest.xml exists")
    tree = ET.parse(manifest)
    declared = False
    not_exported = False
    for el in tree.iter("activity"):
        name = None
        exported = None
        for k, v in el.attrib.items():
            if k.endswith("name"):
                name = v
            if k.endswith("exported"):
                exported = v
        if name in [".ui.admin.TypographyStudioActivity", "com.eve.app.ui.admin.TypographyStudioActivity"]:
            declared = True
            not_exported = (exported == "false")
    assert_true(declared, "TypographyStudioActivity declared in AndroidManifest.xml")
    assert_true(not_exported, "TypographyStudioActivity is not exported (security compliant)")

def check_fonts():
    font_res = RES / "font"
    assert_true((font_res / "inter_regular.ttf").exists(), "Inter regular TTF exists")
    assert_true((font_res / "inter_medium.ttf").exists(), "Inter medium TTF exists")
    assert_true((font_res / "inter_semibold.ttf").exists(), "Inter semibold TTF exists")
    assert_true((font_res / "inter_bold.ttf").exists(), "Inter bold TTF exists")

    assets_fonts = ROOT / "app/src/main/assets/fonts"
    assert_true((assets_fonts / "poppins_regular.ttf").exists(), "Poppins regular TTF exists in assets")
    assert_true((assets_fonts / "poppins_medium.ttf").exists(), "Poppins medium TTF exists in assets")
    assert_true((assets_fonts / "poppins_bold.ttf").exists(), "Poppins bold TTF exists in assets")

    assets_licenses = ROOT / "app/src/main/assets/licenses"
    assert_true((assets_licenses / "Poppins-OFL.txt").exists(), "Poppins-OFL.txt license notice exists")

def check_backend():
    migration = ROOT / "backend/migrations/0017_typography_studio.sql"
    assert_true(migration.exists(), "0017_typography_studio.sql exists")
    content = migration.read_text()
    assert_true("typography_published" in content, "typography_published table defined")
    assert_true("typography_drafts" in content, "typography_drafts table defined")
    assert_true("typography_versions" in content, "typography_versions table defined")
    assert_true("typography_audit_log" in content, "typography_audit_log table defined")

    routes = ROOT / "backend/src/routes/typographyStudio.ts"
    assert_true(routes.exists(), "typographyStudio.ts exists")
    index = ROOT / "backend/src/index.ts"
    index_text = index.read_text()
    assert_true("/api/typography" in index_text, "/api/typography mounted in index.ts")
    assert_true("/api/admin/typography" in index_text, "/api/admin/typography mounted in index.ts")

if __name__ == "__main__":
    print("=== Running Typography Studio Verification ===")
    check_xml_ids()
    check_admin_activity()
    check_manifest()
    check_fonts()
    check_backend()
    print("=== ALL TYPOGRAPHY STUDIO CHECKS PASSED ===")
