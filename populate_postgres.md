
open python connection
```python
import psycopg2
# Connect to an existing database
connection = psycopg2.connect(user="postgres",
                                # password="pynative@#29",
                                # host="127.0.0.1",
                                port="5432",
                                database="sensitivity_attack")

# Create a cursor to perform database operations
cursor = connection.cursor()
# Print PostgreSQL details
print("PostgreSQL server information")
print(connection.get_dsn_parameters(), "\n")
# Executing a SQL query
```

start postgres daemon
```shell
pg_ctl -D /Users/michael/pgsql/data -l logfile start
```

ingest a dataset
```shell
CREATE TABLE unsized_64_u (uid int, val real)   # double precision
COPY arr_u_i(uid, val)
FROM '/Users/michael/openDP/prelim/sensitivity_attacks/unsized_64_u.csv'
DELIMITER ','
```