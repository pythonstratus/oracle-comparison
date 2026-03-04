Here's the summary:

**Key Takeaway:** The `GetRefDt` function shouldn't live in ENTITYDEV — it belongs on the ALS side. But there's a complication: ALS exists in two places (legacy M7 and Exadata), and M7 is getting decommissioned.

**Where ALS lives today:**

- **M7 (legacy):** Has the actively updated ALS with NS&D and other tables. This is where `GetRefDt` should logically come from. But M7 is being decommissioned.
- **Exadata:** Has an ALS user that J created once, but it's **not being updated**. This is the ALS used by the ETL. After modernization, ALS data will reside in Exadata permanently.

**The problem:** You can't just do a simple grant since M7 and Exadata are separate — they don't cross-connect, and there's no ENTITYDEV user in M7. A **DB Link** is needed to bridge them, but that's only a temporary solution since M7 is going away.

**End state after modernization:** ENTITYDEV will use synonyms pointing to ALS tables in Exadata (e.g., `CREATE SYNONYM LD FOR ALS.LD@link`), and eventually the DB link goes away once everything is in Exadata.

**Action items:**

- The DBA script for Christina needs to account for DB Link (not just grants), since the schemas are on different instances
- Brandon has already added some table changes to the ALS Exadata side
- LS (Lien Summary) exists but isn't being updated from ETLs or the ALS application
- Ranjita needs to involve Sam — a call is being scheduled at 11:30 with Sam and Jordan via the cross-team channel to get clarity on the path forward

This changes things from our earlier scripts — sounds like **Option 1 with DB Link** (not just grants) is what's needed short-term, and the Exadata ALS tables need to be brought up to date before M7 goes away. Want me to revise the DBA scripts to reflect this?
