const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_1";
const STAFF_UID = "staff_uid_2";
const OUTSIDER_UID = "outsider_uid_9";
const STORE_ID = "store_2m_001";
const JOIN_CODE = "2MSTORE";

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

async function seedStore() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("stores").doc(STORE_ID).set({
      storeName: "2m Store",
      ownerUid: OWNER_UID,
      joinCode: JOIN_CODE,
      memberUids: [OWNER_UID],
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
    await db.collection("store_invites").doc(JOIN_CODE).set({
      storeId: STORE_ID,
      storeName: "2m Store",
      ownerUid: OWNER_UID,
      createdAt: new Date(Date.now() - 10000),
    });
    await db.collection("stores").doc(STORE_ID).collection("products").doc("prod_1").set({
      storeId: STORE_ID,
      memberUids: [OWNER_UID],
      name: "iPhone 15 Screen",
      category: "Screens",
      sellingPriceEgp: 4500,
      costPriceEgp: 3200,
      stockQty: 5,
      lowStockThreshold: 2,
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });
}

test("Unauthenticated caller cannot read or write store or products", async () => {
  await seedStore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("stores").doc(STORE_ID).get());
  await assertFails(
    unauthDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
  await assertFails(unauthDb.collection("store_invites").get());
});

test("Outsider cannot read store products via direct get or another user's filter", async () => {
  await seedStore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ownQuerySnap = await assertSucceeds(
    outsiderDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .where("memberUids", "array-contains", OUTSIDER_UID)
      .get()
  );
  if (ownQuerySnap.size !== 0) {
    throw new Error(`Expected 0 products for outsider, got ${ownQuerySnap.size}`);
  }
  await assertFails(
    outsiderDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .where("memberUids", "array-contains", OWNER_UID)
      .get()
  );
  await assertFails(
    outsiderDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .doc("prod_1")
      .get()
  );
});

test("Authenticated store member can list products with matching array-contains filter", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertSucceeds(
    ownerDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .where("memberUids", "array-contains", OWNER_UID)
      .get()
  );
});

test("Authenticated user fails unfiltered list query (rules are not filters)", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(
    ownerDb.collection("stores").doc(STORE_ID).collection("products").get()
  );
});

test("Shadow update with ghost field is rejected", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(
    ownerDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("products")
      .doc("prod_1")
      .update({
        stockQty: 4,
        ghostField: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Staff device can look up invite code and join store using matching joinCode", async () => {
  await seedStore();
  const staffDb = testEnv.authenticatedContext(STAFF_UID).firestore();
  await assertSucceeds(staffDb.collection("store_invites").doc(JOIN_CODE).get());
  await assertSucceeds(
    staffDb
      .collection("stores")
      .doc(STORE_ID)
      .update({
        memberUids: [OWNER_UID, STAFF_UID],
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Outsider cannot join store with wrong joinCode or overwrite ownerUid", async () => {
  await seedStore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  await assertFails(
    outsiderDb
      .collection("stores")
      .doc(STORE_ID)
      .update({
        ownerUid: OUTSIDER_UID,
        memberUids: [OWNER_UID, OUTSIDER_UID],
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Sale creation rejects negative price or invalid paymentMethod", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  await assertFails(
    ownerDb
      .collection("stores")
      .doc(STORE_ID)
      .collection("sales")
      .doc("sale_bad")
      .set({
        storeId: STORE_ID,
        memberUids: [OWNER_UID],
        invoiceNumber: "INV-001",
        itemsSummary: "Screen x1",
        items: [{ name: "Screen", qty: 1, unitSell: -50, unitCost: 100 }],
        totalAmountEgp: -50,
        totalCostEgp: 100,
        netProfitEgp: -150,
        paymentMethod: "CRYPTO",
        paidAmountEgp: 0,
        customerName: "",
        customerPhone: "",
        staffId: "staff_0",
        staffName: "المالك",
        createdAt: new Date(Date.now() - 1000),
      })
  );
});
