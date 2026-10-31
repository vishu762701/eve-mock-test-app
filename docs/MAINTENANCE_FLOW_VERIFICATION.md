# Maintenance flow repair

## Evidence and scope

Live baseline was fetched from `main`: `88008c31731d6eb27718a0232b84be537fc938cc`.
Its six-job FULL run is https://github.com/vishu762701/eve-mock-test-app/actions/runs/41209057639.
Public production GET `/api/app-config` returned maintenance enabled at investigation time.
No authenticated production write, Worker deployment or remote migration was performed.

The baseline `TextCheckCell.isChecked` called `TelegramSwitch.setChecked`, which
invoked the user listener during config binding, confirmation and rollback.
The dashboard's enable callback reset the switch to false, invoking disable/save,
then its positive callback set true, reopening confirmation recursively.
Both Android and Worker public config reads also converted failures to default off.

## Implemented contract

- Binding and switch state restoration are silent; deliberate row/switch interaction
  still notifies once. Disabled switch clicks cannot bypass the in-flight guard.
- Dashboard config reads never write or confirm. Until a read succeeds the label
  indicates loading/unavailable; failure permits a read retry. A failed write
  retains the previous display and requires reloading before another write.
- One enable dialog; cancel, Back and outside dismissal restore the last confirmed
  state. The dashboard disables interaction through confirmation and write/readback.
- The dashboard sends only `maintenance_mode`. The config editor sends only fields
  changed from its confirmed snapshot. Both require public readback before success.
- Worker auth and `manage_config_maintenance` permission remain required. Existing
  data and compatibility fields are preserved; supplied values are merged atomically
  into the latest JSON row. Write and audit remain in the existing batch transaction.
  Read/corrupt-storage failure does not fabricate defaults or overwrite the row.
- Home coalesces concurrent checks and cancels them when paused. Startup checks,
  resume and Retry consult public config. Database/network failure cannot dismiss
  maintenance. A persisted last confirmed message is available on offline restart;
  unknown availability has a distinct connection-required notice and Retry.
- The maintenance surface is opaque, scrollable, blocks drawer input and hides
  underlying content from accessibility. A fresh identity-matched Worker admin
  check is required for bypass; a cached client hint cannot grant this bypass.

## Limits and release

This is **Home enforcement**, not server-wide maintenance or exam interruption.
A student already in another activity or active exam is checked when returning to
Home. Existing installed clients retain their old behavior until updated. No schema
migration is needed. Backend changes are staged only and require separately approved
manual production deployment; Android improvements use the existing partial-write
contract even before that deployment. A network-lost write is honestly unconfirmed,
not proof that the server rejected it. Concurrent edits to the same field still use
last writer wins; differing readback produces a reload error rather than false success.

## Verification fixtures

- `MaintenanceSwitchRegressionTest`: real widget binding/click/rollback, restoration,
  disabled repeated clicks; Robolectric execution is distinct from a device run.
- `MaintenanceRepositoryTest`: real repository with fake API transport, on/off payload,
  response acknowledgement, failed/missing/conflicting readback; no production mutation.
- `MaintenanceOverlayNativeTest`: production layout, both themes, small viewport,
  doubled fonts, long Hindi/RTL text, scrolling and working retry click. FULL CI's
  existing package selection includes it in both navigation modes.
- Backend tests execute real public Worker and admin handlers against SQLite with
  actual migrations, authorization, audit and JSON storage, including malformed and
  corrupt-read paths. They do not prove live deployed backend behavior.

Native execution, exact-SHA CI, screenshots and acceptance must be reported from the
actual resulting runs; this document is not itself a claim that those checks passed.
