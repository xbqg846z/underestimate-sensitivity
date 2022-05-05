import numpy as np


def generate_sized_trans(pow):
    return np.zeros(2**pow + 1), np.append(np.zeros(2**pow), 1.)


def generate_sized(pow, target):
    u = np.full(2**pow + 1, 0)
    v = np.insert(np.full(2**pow, 0), target, 1)
    return enumerate(u), enumerate(v)


def generate_unsized_64_trans():
    return _generate_unsized_xx_trans(unsized_64_consts())


def generate_unsized_32_trans():
    return _generate_unsized_xx_trans(unsized_32_consts())


def generate_unsized_64():
    data_size = 2**28
    u = np.random.binomial(n=1, p=0.5, size=data_size)
    v = np.delete(u, 1000)
    return enumerate(u), enumerate(v)


def generate_unsized_32():
    data_size = 2**17
    u = np.random.binomial(n=1, p=0.5, size=data_size)
    v = np.delete(u, 1000)
    return enumerate(u), enumerate(v)





def _generate_unsized_xx_trans(consts):
    U = consts['U']
    m = consts['m']

    pos_val, neg_val = unsized_alternating_values(consts)

    arr_u_start = np.full(m, U)
    arr_v_start = np.full(m - 1, U)

    interleaved = np.empty(m, dtype=np.float64)
    interleaved[0::2] = pos_val
    interleaved[1::2] = neg_val

    arr_u = np.concatenate((arr_u_start, interleaved))
    arr_v = np.concatenate((arr_v_start, interleaved))

    return arr_u, arr_v


def unsized_alternating_values(consts):
    U = consts['U']
    m = consts['m']
    k = consts['k']
    pos_val = +(U * m / 2**k) * (1 / 2 + 2**-k)
    neg_val = -(U * m / 2**k) * (1 / 2 - 2**-k)
    return pos_val, neg_val


def unsized_64_consts():
    return {
        "U": 1,
        "n": 2**28,
        "m": 2**27,
        "k": 52,
        "m_over_2": 2**26
    }


def unsized_32_consts():
    return {
        "U": 1,
        "n": 2**17,
        "m": 2**16,
        "k": 23,
        "m_over_2": 2**15
    }



def write_datasets(u, v, u_path, v_path):
    with open(u_path, 'w') as u_file:
        for uid, val in u:
            u_file.write(f"{uid},{val}\n")

    with open(v_path, 'w') as u_file:
        for uid, val in v:
            u_file.write(f"{uid},{val}\n")


# write unsized 32 bit dataset
# write_datasets(*generate_unsized_32(), "unsized_32_u.csv", "unsized_32_v.csv")

# write unsized transformed 32 bit dataset
u_trans, v_trans = generate_unsized_32_trans()
np.savetxt("unsized_32_trans_u.csv", u_trans)
np.savetxt("unsized_32_trans_v.csv", v_trans)
# print(unsized_64_consts())

# 
u_trans, v_trans = generate_sized_trans(4)
np.savetxt("sized_4_trans_u.csv", u_trans)
np.savetxt("sized_4_trans_v.csv", v_trans)


u, v = generate_sized(6, target=45)
write_datasets(u, v, "sized_6_45_u.csv", "sized_6_45_v.csv")
