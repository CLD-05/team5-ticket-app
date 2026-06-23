import subprocess

# Read seed_users.sql with utf-8-sig to automatically strip BOM
with open('seed_users.sql', 'r', encoding='utf-8-sig') as f:
    sql_content = f.read()

# Run mysql command inside docker container
# We pass the SQL content via stdin
process = subprocess.Popen(
    ['docker', 'exec', '-i', 'ticketing-mysql', 'mysql', '-u', 'root', '-p1234', 'ticketing'],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True
)

stdout, stderr = process.communicate(input=sql_content)

print("STDOUT:", stdout)
print("STDERR:", stderr)
