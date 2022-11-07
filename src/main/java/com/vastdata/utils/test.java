package com.vastdata.utils;

import java.util.HashSet;
import java.util.Set;

public class test {
    public static void main(String[] args) {
        String str = "v$session v$sesstat v$sysstat dba_users " +
                "V$LIBRARYCACHE " +
                "v$sysstat v$sysstat v$sysstat v$sysstat v$sysstat " +
                "v$sysstat " +
                "dba_jobs " +
                "v$parameter " +
                "v$session " +
                "v$sysmetric_history " +
                "v$session_wait " +
                "v$sysstat dual " +
                "v$session " +
                "v$session " +
                "v$session " +
                "gv$sysmetric " +
                "gv$sysmetric " +
                "gv$sysmetric " +
                "v$sysstat " +
                "gv$lock gv$session gv$sqlarea gv$session_wait gv$lock " +
                "v$sql " +
                "mon_dba_extents " +
                "V$SGASTAT " +
                "v$sysstat " +
                "v$sysstat " +
                "v$process " +
                "v$sysstat " +
                "V$SQLAREA " +
                "V$SQLAREA " +
                "V$SQLAREA " +
                "gv$sysmetric " +
                "v$session V$SQLAREA " +
                "gv$lock gv$locked_object dba_objects gv$session gv$lock " +
                "V$SQLAREA " +
                "gv$session " +
                "v$sql_plan V$ACTIVE_SESSION_HISTORY " +
                "V$BUFFER_POOL_STATISTICS " +
                "gv$resource_limit " +
                "gv$sysmetric " +
                "v$sysstat " +
                "dba_jobs " +
                "dba_scheduler_jobs " +
                "GV$BACKUP_SET v$backup_datafile dual " +
                "v$session " +
                "v$session " +
                "v$session " +
                "v$session " +
                "v$session " +
                "v$session " +
                "v$session " +
                "v$session dba_objects " +
                "v$session " +
                "gv$session dba_tables dba_users " +
                "dba_tab_partitions dba_users " +
                "dba_tables dba_users v$parameter dba_tab_partitions " +
                "V$LIBRARYCACHE " +
                "dba_users dba_segments dba_indexes " +
                "dba_indexes dba_ind_partitions " +
                "dba_indexes dba_ind_partitions " +
                "V$DATABASE_BLOCK_CORRUPTION " +
                "v$recover_file " +
                "v$resource_limit " +
                "dba_tables " +
                "dba_segments " +
                "dba_users " +
                "v$session_longops " +
                "V$RMAN_BACKUP_JOB_DETAILS " +
                "dba_tables " +
                "dba_indexes " +
                "SYS.WRH$_SEG_STAT DBA_OBJECTS DBA_HIST_SNAPSHOT " +
                "V$MEMORY_TARGET_ADVICE " +
                "v$asm_diskgroup " +
                "v$diag_alert_ext oneday " +
                "oracle_table_space_usage_trend " +
                "V$SQLAREA " +
                "gv$active_session_history " +
                "gv$lock gv$session gv$sqlarea gv$session_wait " +
                "dba_data_files dba_free_space v$temp_space_header ";
        String[] tables = str.split(" ");

        Set<String> tab = new HashSet<>();
        for(String table :tables){
            tab.add(table);
        }
                System.out.println(tab.toString());
    }
}
