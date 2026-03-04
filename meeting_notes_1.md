## UAT Progress Meeting Summary — March 4, 2026

**Overall Status:** The team is in the final push toward UAT with a critical window closing by end of business Friday. Progress is largely dependent on external blockers — primarily Matthew (DBA/DDL work), Alia (service accounts/APIs), and middleware support — rather than internal development gaps.

---

**Key Blockers & Dependencies**

The biggest dependency chain runs through Matthew: the DIAL DDLs need to be pushed to the DBA before the DIAL ETL can run, and that ETL must complete before ICSE work can proceed. Tomorrow is effectively the drop-dead date for this. Sam is keeping the team updated. Separately, the "allergic table" / golden key question also sits with Matthew — Ranjita and Diane are drafting that email to unblock it and generate the associated tickets.

**ETLs & Data Seeding**

Legacy data seeding has been completed for all ETLs except DIAL (blocked by Matthew). Once that's resolved, a clean run of all ETLs is planned for the weekend. Splunk setup and alert configuration also remain in progress — Chinmaya (Chamya) is working through what's still needed, though there's a complication around staging: cron jobs are not permitted there, meaning the ALS/NTDS customer account config will need to move directly to prod for testing, requiring the appropriate forms to be submitted first.

**Ansible & Promotion Validation**

Ansible is working. The current blocker is verifying that recently promoted components have been properly tested. A middleware support ticket was opened due to a 500 error in the BoE/POE test environment. The team is waiting on a response before calling the promotion validated.

**Entitlements & Service Accounts**

BEARS test entitlements are largely in place, and the document for requesting access is being circulated. However, the service account for DOE is still not visible to the VOE team, blocking formal entitlement creation. A dummy account is currently in place for UAT and could technically support a production promotion if needed, though it's acknowledged as less official. Eddie reached out Monday and will follow up today — the team is waiting before escalating further.

**ICS / 226 Forms**

Not a UAT blocker — folders can be added manually in the interim.

---

## Temporary Routing (SSO for Prod)

This topic came up in the context of getting SSO configured by Friday as a prerequisite for scheduling UAT activities.

**The approach:** A temporary container/route is being created — similar to what was done during the earlier integration test — to serve as the SSO host endpoint. The reason a temporary route is needed is that a proper host endpoint cannot be created until deployments are done, routes are configured, and the HTTPS certificate is installed. The temporary route bridges the gap in the meantime.

**The plan for cutover:** When the actual production deployment is done, the temporary route will be removed and replaced with the permanent route that matches the SSO provider's configured source.

**Risk assessment:** The team assessed the risk of this swap not going as planned. The conclusion was that there is **no meaningful risk**. The Apache server used for the temporary container serves only one purpose — hosting a single HTML page to demonstrate that the SSO integration functions correctly (essentially completing the integration test). It is not part of the application's real traffic path. When the permanent deployment is made with the real route name matching the SSO configuration, the temporary container can simply be removed. No data or application functionality depends on it.
