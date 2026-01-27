-- Oracle Legacy vs Modern Exadata Comparison Queries
-- ==================================================
-- These queries use MINUS to find differences between ALS_LEGACY_REPLICA (Golden Gate)
-- and the local schema tables.
-- Each table has two queries: one for rows only in local, one for rows only in legacy.

-- =============================================
-- ACTDELETE Table
-- =============================================

-- @name: ACTDELETE_Local_Only
-- @description: Rows in local ACTDELETE but not in ALS_LEGACY_REPLICA.ACTDELETE
SELECT * FROM ACTDELETE
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ACTDELETE;

-- @name: ACTDELETE_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ACTDELETE but not in local ACTDELETE
SELECT * FROM ALS_LEGACY_REPLICA.ACTDELETE
MINUS
SELECT * FROM ACTDELETE;

-- =============================================
-- ACTUNDO Table
-- =============================================

-- @name: ACTUNDO_Local_Only
-- @description: Rows in local ACTUNDO but not in ALS_LEGACY_REPLICA.ACTUNDO
SELECT * FROM ACTUNDO
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ACTUNDO;

-- @name: ACTUNDO_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ACTUNDO but not in local ACTUNDO
SELECT * FROM ALS_LEGACY_REPLICA.ACTUNDO
MINUS
SELECT * FROM ACTUNDO;

-- =============================================
-- ENT Table
-- =============================================

-- @name: ENT_Local_Only
-- @description: Rows in local ENT but not in ALS_LEGACY_REPLICA.ENT
SELECT * FROM ENT
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ENT;

-- @name: ENT_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ENT but not in local ENT
SELECT * FROM ALS_LEGACY_REPLICA.ENT
MINUS
SELECT * FROM ENT;

-- =============================================
-- ENTACT Table
-- =============================================

-- @name: ENTACT_Local_Only
-- @description: Rows in local ENTACT but not in ALS_LEGACY_REPLICA.ENTACT
SELECT * FROM ENTACT
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ENTACT;

-- @name: ENTACT_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ENTACT but not in local ENTACT
SELECT * FROM ALS_LEGACY_REPLICA.ENTACT
MINUS
SELECT * FROM ENTACT;

-- =============================================
-- ENTEMP Table
-- =============================================

-- @name: ENTEMP_Local_Only
-- @description: Rows in local ENTEMP but not in ALS_LEGACY_REPLICA.ENTEMP
SELECT * FROM ENTEMP
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ENTEMP;

-- @name: ENTEMP_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ENTEMP but not in local ENTEMP
SELECT * FROM ALS_LEGACY_REPLICA.ENTEMP
MINUS
SELECT * FROM ENTEMP;

-- =============================================
-- ENTEMP2 Table (Note: Can be ignored per comments - Golden Gate doesn't have entemp2)
-- =============================================

-- @name: ENTEMP2_Local_Only
-- @description: Rows in local ENTEMP2 but not in ALS_LEGACY_REPLICA.ENTEMP2 (may not exist in GG)
SELECT * FROM ENTEMP2
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ENTEMP2;

-- @name: ENTEMP2_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ENTEMP2 but not in local ENTEMP2 (may not exist in GG)
SELECT * FROM ALS_LEGACY_REPLICA.ENTEMP2
MINUS
SELECT * FROM ENTEMP2;

-- =============================================
-- ENTMOD Table
-- =============================================

-- @name: ENTMOD_Local_Only
-- @description: Rows in local ENTMOD but not in ALS_LEGACY_REPLICA.ENTMOD
SELECT * FROM ENTMOD
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.ENTMOD;

-- @name: ENTMOD_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.ENTMOD but not in local ENTMOD
SELECT * FROM ALS_LEGACY_REPLICA.ENTMOD
MINUS
SELECT * FROM ENTMOD;

-- =============================================
-- EOM Table
-- =============================================

-- @name: EOM_Local_Only
-- @description: Rows in local EOM but not in ALS_LEGACY_REPLICA.EOM
SELECT * FROM EOM
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.EOM;

-- @name: EOM_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.EOM but not in local EOM
SELECT * FROM ALS_LEGACY_REPLICA.EOM
MINUS
SELECT * FROM EOM;

-- =============================================
-- LOGLOAD Table (with ORDER BY)
-- =============================================

-- @name: LOGLOAD_Local_Only
-- @description: Rows in local LOGLOAD but not in ALS_LEGACY_REPLICA.LOGLOAD
SELECT * FROM LOGLOAD
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.LOGLOAD;

-- @name: LOGLOAD_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.LOGLOAD but not in local LOGLOAD
SELECT * FROM ALS_LEGACY_REPLICA.LOGLOAD
MINUS
SELECT * FROM LOGLOAD;

-- =============================================
-- TIMENON Table
-- =============================================

-- @name: TIMENON_Local_Only
-- @description: Rows in local TIMENON but not in ALS_LEGACY_REPLICA.TIMENON
SELECT * FROM TIMENON
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.TIMENON;

-- @name: TIMENON_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.TIMENON but not in local TIMENON
SELECT * FROM ALS_LEGACY_REPLICA.TIMENON
MINUS
SELECT * FROM TIMENON;

-- =============================================
-- TIMETIN Table
-- =============================================

-- @name: TIMETIN_Local_Only
-- @description: Rows in local TIMETIN but not in ALS_LEGACY_REPLICA.TIMETIN
SELECT * FROM TIMETIN
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.TIMETIN;

-- @name: TIMETIN_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.TIMETIN but not in local TIMETIN
SELECT * FROM ALS_LEGACY_REPLICA.TIMETIN
MINUS
SELECT * FROM TIMETIN;

-- =============================================
-- TRANTRAIL Table
-- =============================================

-- @name: TRANTRAIL_Local_Only
-- @description: Rows in local TRANTRAIL but not in ALS_LEGACY_REPLICA.TRANTRAIL
SELECT * FROM TRANTRAIL
MINUS
SELECT * FROM ALS_LEGACY_REPLICA.TRANTRAIL;

-- @name: TRANTRAIL_Legacy_Only
-- @description: Rows in ALS_LEGACY_REPLICA.TRANTRAIL but not in local TRANTRAIL
SELECT * FROM ALS_LEGACY_REPLICA.TRANTRAIL
MINUS
SELECT * FROM TRANTRAIL;
