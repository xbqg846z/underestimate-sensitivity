from generate_datasets import (
    generate_unsized_64,
    generate_sized_trans,
    generate_unsized_64_trans,
    unsized_64_consts,
    unsized_alternating_values,
)

import sqlite3

con = sqlite3.connect("database.sqlite3")
cur = con.cursor()


def setup_sized_4_trans():
    cur.execute("CREATE TABLE sized_64_4_trans_u (col1 real);")
    cur.execute("CREATE TABLE sized_64_4_trans_v (col1 real);")
    con.commit()

    u, v = generate_sized_trans(4)
    cur.executemany(
        "INSERT INTO sized_64_4_trans_u (col1) VALUES (?);", u[:, None].tolist()
    )
    con.commit()
    cur.executemany(
        "INSERT INTO sized_64_4_trans_v (col1) VALUES (?);", v[:, None].tolist()
    )
    con.commit()

def setup_unsized_64_trans():
    cur.execute("CREATE TABLE unsized_64_trans_u (col1);")
    cur.execute("CREATE TABLE unsized_64_trans_v (col1);")
    con.commit()

    u, v = generate_unsized_64_trans()
    cur.executemany(
        "INSERT INTO unsized_64_trans_u (col1) VALUES (?);", u[:, None].tolist()
    )
    con.commit()
    cur.executemany(
        "INSERT INTO unsized_64_trans_v (col1) VALUES (?);", v[:, None].tolist()
    )
    con.commit()


def setup_unsized_64():
    cur.execute("CREATE TABLE unsized_64_u (uid, val);")
    cur.execute("CREATE TABLE unsized_64_v (uid, val);")
    con.commit()

    u, v = generate_unsized_64()
    cur.executemany(
        "INSERT INTO unsized_64_u (uid, val) VALUES (?, ?);", enumerate(u.tolist())
    )
    con.commit()
    cur.executemany(
        "INSERT INTO unsized_64_v (uid, val) VALUES (?, ?);", enumerate(v.tolist())
    )
    con.commit()


def direct_sum():
    sized_64_u_sum = cur.execute("SELECT SUM(col1) FROM sized_64_trans_u").fetchall()[
        0
    ][0]
    sized_64_v_sum = cur.execute("SELECT SUM(col1) FROM sized_64_trans_v").fetchall()[
        0
    ][0]
    print("Exact sums of neighboring datasets on Sqlite.")
    print(sized_64_u_sum, sized_64_v_sum)


def trans_sum():
    consts = unsized_64_consts()
    m, U = consts["m"], consts["U"]
    pos_val, neg_val = unsized_alternating_values(consts)

    attack_uid = 5

    # direct sum of transformed dataset
    cur.execute(
        """
    SELECT 
        SUM(CASE 
            WHEN uid < ? THEN ?
            WHEN uid = ? THEN val * ?
            WHEN uid % 2 = 1 THEN ?
            WHEN uid % 2 = 0 THEN ?
        END)
    FROM unsized_64_u""",
        (m, U, m, U, pos_val, neg_val),
    ).fetchall()

    # clamped sum of transformed dataset
    lower, upper = neg_val, U
    cur.execute(
        """SELECT 
        SUM(CASE
            WHEN trans < ? THEN ?
            WHEN trans > ? THEN ?
            ELSE trans
        END)
    FROM (SELECT 
        CASE 
            WHEN uid < ? THEN ?
            WHEN uid == ? THEN val * ?
            WHEN uid % 2 = 1 THEN ?
            WHEN uid % 2 = 0 THEN ?
        END AS trans
    FROM unsized_64_u)""",
        (lower, lower, upper, upper, attack_uid, U, attack_uid, U, pos_val, neg_val),
    ).fetchall()
