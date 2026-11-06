#!/bin/sh
# moments4j entrypoint — same commands as the original flask CLI (core/commands.py)
#
#   docker run moments4j                 -> start the web server
#   docker run moments4j init-app        -> create tables + roles/permissions
#   docker run moments4j lorem --user=10 -> generate fake data
#   docker run moments4j init-db --drop  -> recreate empty schema
set -e

CMD="${1:-}"

case "$CMD" in
  # no args or a JVM/app flag: start the server (pass any extra args through)
  ""|-*) exec java $MAVEN_OPTS -jar app.jar "$@" ;;
  # known CLI commands: run them, then exit (container lifecycle = the command)
  init-db|init-app|lorem)
    java $MAVEN_OPTS -jar app.jar "$@"
    exit 0
    ;;
  # anything else: assume it's a JVM/app option meant for the server, pass through
  *)
    exec java $MAVEN_OPTS -jar app.jar "$@"
    ;;
esac
