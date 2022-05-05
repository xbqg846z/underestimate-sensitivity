from generate_datasets import generate_sized_64_trans

u, v = generate_sized_64_trans()

# numpy pairwise summation not susceptible: https://github.com/numpy/numpy/pull/3685
print("Exact sums of neighboring datasets on Numpy.")
print(u.sum(), v.sum())

# python's sum is vulnerable
print("Exact sums of neighboring datasets using built-in python function.")
print(sum(u), sum(v))

# https://docs.python.org/3/library/math.html#math.fsum
import math
math.fsum(u)
