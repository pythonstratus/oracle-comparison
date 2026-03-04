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


## Email

Subject: ALS Management Meeting Summary – March 4, 2026

Hi Team,

Here's a summary of today's discussion on ALS management, the GetRefDt function, and the path forward.

Key Decisions

1. GetRefDt belongs on the ALS side, not in ENTITYDEV. It should logically come from the legacy ALS, but since M7 is being decommissioned, we need an interim approach.

2. Ignore the Exadata ALS snapshot for now. The ALS user Jay created in Exadata is a stale snapshot from last year and isn't being updated. We'll point to the ALS legacy replica instead.

3. Use synonyms for decoupling. We'll create synonyms in ENTITY pointing to the ALS legacy replica tables (e.g., CREATE SYNONYM XE FOR ALS_LEGACY_REPLICA.XE). When ALS modernization Phase 2 goes live, we'll run a script to rewrite those synonyms to point to the new Exadata tables. This keeps the code change minimal — just a synonym swap.

4. For now, change ALS references to ALS legacy replica references. We can keep the old table names to make our lives easier, but wrap them with synonyms. When ALS modernization is closer to done, synonyms will be updated to point to the new table names.

Golden Gate Replication

- XE, LS, and LD are already replicated via Golden Gate.
- LA is missing and needs to be added. Ranjita will email Matthew with the table name and requirements. Matthew may need a ticket for this.
- XE and LS get updated roughly once a month (last update was around Feb 2nd).
- Tables available in the legacy replica: LS, LD, FA, and XE (no LA currently).
- Tables are present in both DEV and TEST, but TEST access is currently down due to an issue Matthew needs to fix.

OWL / ALS Modernization

- Jordan's team (OWL) is renaming tables as part of modernization. The OWL team was informed years ago that ENTITY depends on these tables.
- Reference data is not going away. Parveen knows what GetRefDt maps to in the modernized schema.
- Harveen's work won't be limited to XE, LA, or LD — likely a broader summary. The team can keep LS and LD but needs to create synonyms. When ALS deploys, an install script will be needed to update synonyms.

Action Items

- Ranjita: Email Matthew to add the LA table to Golden Gate replication (may need a ticket).
- Ranjita: Share the agreed-upon approach via email with the full team.
- Speaker: Explore and prepare the synonym creation scripts for ENTITY.
- Santosh: Continue analyzing OWL dependencies (one dependency found so far, but there are likely more references across all entity code).
- Team: Acknowledge that all ALS references across entity code need this synonym treatment — this decoupling should have started months ago per prior discussions.

Next call to be scheduled as needed. Let me know if I missed anything.

Thanks
