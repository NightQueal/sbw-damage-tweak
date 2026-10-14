"""Copy known dependencies into this checkout; never modify the Minecraft instance."""
import argparse, pathlib, json, hashlib, zipfile
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument("instance",type=pathlib.Path,help="Minecraft instance containing mods/")
parser.add_argument("--with-explosion-overhaul",action="store_true",help="Include a copy even if its instance jar is disabled")
args=parser.parse_args();root=pathlib.Path(__file__).resolve().parent.parent
rows=json.loads((root/"scripts/dependencies.json").read_text())
prepared=[]
for row in rows:
 if row.get("optional") and not args.with_explosion_overhaul:continue
 src=args.instance/"mods"/row["source"]
 if not src.exists() and row["source"].endswith(".disabled"):src=args.instance/"mods"/row["source"].removesuffix(".disabled")
 if not src.is_file():raise SystemExit("Missing dependency: "+row["source"])
 if row.get("embedded"):
  with zipfile.ZipFile(src) as z:data=z.read(row["embedded"])
 else:data=src.read_bytes()
 if hashlib.sha256(data).hexdigest()!=row["sha256"]:raise SystemExit("Version/hash mismatch: "+row["source"])
 prepared.append((row["local"],data))
(root/"runtime-mods").mkdir(exist_ok=True)
for name,data in prepared:(root/"runtime-mods"/name).write_bytes(data)
print("Prepared",len(prepared),"local dependency jars. The source instance was not modified.")
