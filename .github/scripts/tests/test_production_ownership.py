"""Guard concrete production owners without banning adapters or parallel renderers.

Runs in existing FAST unittest discovery; no Android emulator required.
"""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
PRODUCTION = ROOT / "app/src/main/java/com/eve/app"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
ANDROID = "{http://schemas.android.com/apk/res/android}"


def source(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def declared_activities():
    return ET.parse(MANIFEST).getroot().findall("./application/activity")


def activity_name(activity):
    return activity.get(ANDROID + "name")


class ProductionOwnershipTest(unittest.TestCase):
    def test_one_registration_per_production_activity(self):
        names = [activity_name(node) for node in declared_activities()]
        self.assertEqual(len(names), len(set(names)),
                         "Duplicate manifest Activity registrations create competing entry points")
        for owner in (
            ".ui.login.LoginActivity",
            ".ui.home.MainActivity",
            ".ui.profile.ProfileActivity",
            ".ui.profile.UsernameCardActivity",
            ".ui.profile.EveQrLiveScannerActivity",
            ".ui.notifications.NotificationsActivity",
            ".ui.admin.AdminActivity",
            ".ui.test.TestActivity",
        ):
            self.assertEqual(names.count(owner), 1, owner)

    def test_one_launcher_and_one_public_username_route(self):
        launcher, username_routes = [], []
        for activity in declared_activities():
            for intent_filter in activity.findall("intent-filter"):
                actions = {node.get(ANDROID + "name")
                           for node in intent_filter.findall("action")}
                categories = {node.get(ANDROID + "name")
                              for node in intent_filter.findall("category")}
                if ("android.intent.action.MAIN" in actions and
                        "android.intent.category.LAUNCHER" in categories):
                    launcher.append(activity_name(activity))
                if "android.intent.action.VIEW" in actions:
                    for data in intent_filter.findall("data"):
                        if (data.get(ANDROID + "scheme") == "eve-mock-test" and
                                data.get(ANDROID + "host") == "username"):
                            username_routes.append(activity_name(activity))
        self.assertEqual(launcher, [".ui.login.LoginActivity"])
        self.assertEqual(username_routes, [".ui.profile.UsernameCardActivity"])

    def test_personal_qr_and_scanner_export_contracts(self):
        owners = {activity_name(node): node for node in declared_activities()}
        self.assertEqual(owners[".ui.profile.UsernameCardActivity"].get(
            ANDROID + "exported"), "true")
        self.assertEqual(owners[".ui.profile.EveQrLiveScannerActivity"].get(
            ANDROID + "exported"), "false")
        self.assertEqual(owners[".ui.profile.ProfileActivity"].get(
            ANDROID + "exported"), "false")
        profile = source("app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt")
        viewer = source("app/src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt")
        scanner = source("app/src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt")
        self.assertIn("Intent(this, UsernameCardActivity::class.java)", profile)
        self.assertIn("Intent(this, EveQrLiveScannerActivity::class.java)", viewer)
        self.assertIn("ProfileQrPayload.parse(", viewer)
        self.assertIn("ProfileQrPayload.parse(", scanner)

    def test_only_inbox_acknowledges_notifications(self):
        # Store owns the mutation. Only the inbox can invoke it, not Home or a
        # second notification handler. Ignore tests and other non-production trees.
        call = re.compile(r"\bNotificationStore\s*\.\s*markAllRead\s*\(")
        callers = set()
        for extension in ("*.kt", "*.java"):
            for file in PRODUCTION.rglob(extension):
                if call.search(file.read_text(encoding="utf-8")):
                    callers.add(file.relative_to(PRODUCTION).as_posix())
        self.assertEqual(
            callers, {"ui/notifications/NotificationsActivity.kt"},
            "Unread acknowledgement must be owned by the visible inbox",
        )
        self.assertIn(
            "NotificationStore.hasUnread(this)",
            source("app/src/main/java/com/eve/app/ui/home/MainActivity.kt"),
        )

    def test_generation_retry_keeps_existing_monitor_dispatch_owner(self):
        call = re.compile(r"\.retryGenerationJob\s*\(")
        callers = {
            file.relative_to(PRODUCTION).as_posix()
            for file in PRODUCTION.rglob("*.kt")
            if call.search(file.read_text(encoding="utf-8"))
        }
        self.assertEqual(callers, {"ui/admin/SystemMonitorActivity.kt"})
        owners = {activity_name(node): node for node in declared_activities()}
        self.assertEqual(owners[".ui.admin.SystemMonitorActivity"].get(
            ANDROID + "exported"), "false")

    def test_home_filter_uses_viewmodel_as_selected_state_owner(self):
        vm = source("app/src/main/java/com/eve/app/ui/home/HomeViewModel.kt")
        home = source("app/src/main/java/com/eve/app/ui/home/MainActivity.kt")
        self.assertIn("private val _selectedCategory = MutableStateFlow(", vm)
        self.assertIn("val selectedCategory: StateFlow<String>", vm)
        self.assertIn("currentSelection = { viewModel.selectedCategory.value }", home)
        self.assertIn("HomeCategorySelection.select(", vm)

    def test_api_usage_has_one_repository_transport_owner(self):
        call = re.compile(r"\.getApiUsage\s*\(")
        callers = {
            file.relative_to(PRODUCTION).as_posix()
            for file in PRODUCTION.rglob("*.kt")
            if call.search(file.read_text(encoding="utf-8"))
        }
        self.assertEqual(callers, {"data/repository/ApiUsageRepository.kt"})


if __name__ == "__main__":
    unittest.main()
