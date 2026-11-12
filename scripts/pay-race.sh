#!/usr/bin/env bash
# Manual check for the double-charge bug: holds two seats, then sends 5 "Pay" requests
# for the same booking at the same moment. Expected with the fix: one 200 and four 409s,
# and exactly one "Simulated charge" line in the backend log.
#
# Usage: ./scripts/pay-race.sh   (the app must be running: docker compose up -d)
set -euo pipefail

BASE=${BASE:-http://localhost:3000}
EVENT_ID=${EVENT_ID:-5}
PARALLEL=${PARALLEL:-5}

json() { python3 -c "import sys, json; d = json.load(sys.stdin); print($1)"; }

TOKEN=$(curl -sf -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"demo@plass.dev","password":"Demo1234!"}' | json 'd["token"]')

SEATS=$(curl -sf "$BASE/api/events/$EVENT_ID/seats" \
  | json '",".join([str(s["id"]) for s in d if s["status"] == "AVAILABLE"][:2])')

BOOKING=$(curl -sf -X POST "$BASE/api/bookings" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{\"eventId\":$EVENT_ID,\"seatIds\":[$SEATS]}")
BOOKING_ID=$(echo "$BOOKING" | json 'd["id"]')
REFERENCE=$(echo "$BOOKING" | json 'd["reference"]')
echo "Held seats $SEATS -> booking $BOOKING_ID ($REFERENCE)"
echo "Sending $PARALLEL payments at the same time..."

for _ in $(seq 1 "$PARALLEL"); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE/api/bookings/$BOOKING_ID/pay" \
    -H "Authorization: Bearer $TOKEN" &
done | sort | uniq -c | sed 's/^/  HTTP /'
wait

sleep 1
CHARGES=$(docker compose logs backend 2>/dev/null | grep -c "Simulated charge .* for $REFERENCE" || true)
echo "Charges for $REFERENCE: $CHARGES"
if [ "$CHARGES" = "1" ]; then echo "OK: charged once"; else echo "BUG: charged $CHARGES times"; fi
