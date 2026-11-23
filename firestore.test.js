const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_jarvis_01";
const BOB_UID = "bob_jarvis_02";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users are denied across all Jarvis collections", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("chats").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("profile").doc("settings").get());
});

test("Profile settings: owner create, partial update, shadow field & future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("settings").set({
      userId: ALICE_UID,
      ownerDisplayName: "aarif khann",
      primaryLanguage: "en",
      memoryEnabled: true,
      selectedVoiceId: "m_cinematic_en",
      voiceSpeechRate: 1.0,
      voicePitch: 1.0,
      favoriteVoiceIds: ["m_cinematic_en"],
      securityPinConfigured: false,
      biometricEnabled: false,
      secureChatEnabled: true,
      voicePassphraseEnabled: false,
      emergencyLocked: false,
      installedVersion: "1.0.0",
      lastUpdateReviewIso: "2026-11-23",
      nextUpdateReviewIso: "2027-03-23",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("settings").update({
      primaryLanguage: "hi",
      voiceSpeechRate: 1.15,
      updatedAt: pastDate(1000),
    })
  );

  // Shadow field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("settings").update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt modification rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("settings").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profile").doc("settings").update({
      primaryLanguage: "en",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or mutate Alice's tasks, chats, or memories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Review JARVIS security",
      notes: "Enable biometric lock",
      category: "task",
      dueLabel: "Today 6:00 PM",
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").update({
      completed: true,
      updatedAt: pastDate(1000),
    })
  );
});

test("Chats, memories, notes, visionHistory, codingProjects, and updateHistory CRUD + terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(4000);

  // Chat message create + reject @DocumentId 'id'
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("msg_1").set({
      userId: ALICE_UID,
      role: "user",
      content: "Namaste JARVIS",
      language: "hi",
      center: "brain",
      encrypted: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chats").doc("msg_2").set({
      id: "msg_2",
      userId: ALICE_UID,
      role: "user",
      content: "Ghost id field",
      language: "en",
      center: "brain",
      encrypted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Personal memory create & list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "Preference",
      content: "Prefers Hindi and English explanations",
      encrypted: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("memories").get());

  // Note create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      title: "Architecture",
      content: "Golden Holographic UI with Jetpack Compose",
      tag: "Dev",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").update({
      title: "Architecture v2",
      updatedAt: pastDate(1000),
    })
  );

  // Vision history create & toggle savedLocally
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("visionHistory").doc("vis_1").set({
      userId: ALICE_UID,
      mode: "generation",
      prompt: "Golden holographic arc reactor",
      resultSummary: "Generated 1K golden holographic image",
      savedLocally: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("visionHistory").doc("vis_1").update({
      savedLocally: true,
      updatedAt: pastDate(1000),
    })
  );

  // Coding project create
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("codingProjects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "Kotlin Coroutines Helper",
      language: "Kotlin",
      category: "Code Generation",
      prompt: "Create a flow retry helper",
      codeOutput: "fun <T> Flow<T>.retrySafe() = retry(3)",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Update history terminal state lock: reviewed -> approved succeeds; approved -> reviewed fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("updateHistory").doc("upd_1").set({
      userId: ALICE_UID,
      version: "1.1.0",
      summary: "4-month compatibility and security review",
      status: "reviewed",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("updateHistory").doc("upd_1").update({
      status: "approved",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("updateHistory").doc("upd_1").update({
      status: "reviewed",
      updatedAt: pastDate(500),
    })
  );
});
