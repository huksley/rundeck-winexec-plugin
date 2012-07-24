Windows execution plugin for rundeck
====================================

WARNING: Don`t use at production! This is ALPHA quality.

Latest news, code, see at
https://github.com/huksley/rundeck-winexec-plugin

Executes using j-interop library, svcctl RPC calls. All java, works everythere.

Installs service on remote Windows host for execution,
which responds on 1989 (by default) port for HTTP
commands.

Removes services on shutdown.

====== BUILDING

1. ant clean jar

====== INSTALLATION

1. Copy rundeck-winexec-plugin-*.jar to RUNDECK_HOME/libexec/
2. Copy win32exec.properties to RUNDECK_HOME/etc
3. Modify win32exec.properties to suit your needs

====== UPGRADE

1. Stop rundeck
2. Delete RUNDECK_HOME/libexec/cache
3. Delete RUNDECK_HOME/libexec/rundeck-winexec-plugin-*.jar
4. Copy rundeck-winexec-plugin-*.jar to RUNDECK_HOME/libexec/
5. Start rundeck
