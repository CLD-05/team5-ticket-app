import subprocess

sql_query = "UPDATE users SET password = '$2a$10$3pL9yHhxvicED.TRdyOeq.iLvpxmwwskr9BHHyxF0QFc2MNkxjYim' WHERE email LIKE 'user%';"

process = subprocess.Popen(
    ['docker', 'exec', '-i', 'ticketing-mysql', 'mysql', '-u', 'root', '-p1234', 'ticketing'],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    stderr=subprocess.PIPE,
    text=True
)

stdout, stderr = process.communicate(input=sql_query)

print("STDOUT:", stdout)
print("STDERR:", stderr)
