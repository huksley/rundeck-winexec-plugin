Windows execution plugin for rundeck
====================================

WARNING: Don`t use at production! This is ALPHA quality.

Latest news, code, see at
https://github.com/huksley/rundeck-winexec-plugin

Executes using j-interop library, svcctl RPC calls. All java, works everythere.

Installs service on remote Windows host for execution, which responds on 1989 (by default) port for HTTP commands.
Removes services on shutdown.
Checked on Windows 2003 SP2 EE, Windows 2008.

====== BUILDING

1. ant clean jar

====== INSTALLATION

1. Copy rundeck-winexec-plugin-*.jar to RUNDECK_HOME/libexec/
2. Copy winexec.properties to RUNDECK_HOME/etc
3. Modify winexec.properties to suit your needs

====== UPGRADE

1. Stop rundeck
2. Delete RUNDECK_HOME/libexec/cache
3. Delete RUNDECK_HOME/libexec/rundeck-winexec-plugin-*.jar
4. Copy rundeck-winexec-plugin-*.jar to RUNDECK_HOME/libexec/
5. Start rundeck

====== NODE REQUIREMENTS =====

1. Java 1.5+ in classpath
2. Firewall allows java to bind to port
3. Free port 1989 opened (or you can override using winexec.properties)

====== LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

====== INCLUDED COMPONENTS

A number of additional software components included in this plugin.

jcifs.jar - jcifs.samba.org - LGPL
jcifs-deps - ? - ?
Tanuki Software Wrapper - http://www.tanukisoftware.com/ - GPL v.2
