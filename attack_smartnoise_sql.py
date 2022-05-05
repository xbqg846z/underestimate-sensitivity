from snsql import Privacy
from snsql.sql.reader.base import SqlReader
from snsql.sql.private_reader import PrivateReader

# high epsilon to reveal distance between u and v
privacy = Privacy(epsilon=100.0, delta=0.01)

# engine doesn't get passed down into the internal functions
# import mysql.connector
# connection = mysql.connector.connect(user='root',
#                               host='127.0.0.1',
#                               port='3306',
#                               database='sensitivity_attack')
# reader = snsql.from_connection(connection, privacy=privacy, engine='mysql', metadata="unsized_64_trans.yml")


import sqlite3
connection = sqlite3.connect("database.sqlite3")

# cannot use sqlite or mysql. Seems like it was at least partially implemented, (in peek) but is not currently available
reader = PrivateReader(
    SqlReader.from_connection(connection, engine="sqlite", metadata="unsized_64_trans.yml", privacy=privacy), 
    "unsized_64_trans.yml", 
    privacy=privacy)

print(reader.execute('SELECT SUM(col1) FROM unsized_64_trans_u'))
print(reader.execute('SELECT SUM(col2) FROM unsized_64_trans_v'))

# smartnoise sql also disallows transformations
# result = reader.execute(f"""SELECT SUM(trans) FROM (
#     SELECT CASE 
#         WHEN uid < {m} THEN {U}
#         WHEN uid == {m} THEN val * {U}
#         WHEN uid % 2 = 1 THEN {pos_val}
#         ELSE {neg_val}
#     END as trans FROM arr_u_uid.arr_u_uid)""")
