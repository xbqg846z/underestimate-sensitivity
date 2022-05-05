using System;
using System.Linq;

using PINQ;
using System.IO;

namespace TestHarness
{
    public class TestHarness
    {
        public class PINQAgentLogger : PINQAgent
        {
            string name;
            double total;
            public override bool apply(double epsilon)
            {
                total += epsilon;
                Console.WriteLine("**privacy change**\tepsilon: " + epsilon.ToString("0.00") + "\ttotal: " + total.ToString("0.00") + "\t(" + name + ")");
                Console.WriteLine("**privacy change**\tepsilon: {0:F2}\ttotal: {1:F2}\t({2})", epsilon, total, name);

                return true;
            }

            public PINQAgentLogger(string n) { name = n; }
        }

        static void Main(string[] args)
        {
            TestHarness.WithoutRowTransform();
            //TestHarness.WithRowTransform();
        }

        static void WithRowTransform()
        {
            // Out of memory
            TestHarness.WithRowTransformAttack(@"../../../../unsized_64_u.csv");
            TestHarness.WithRowTransformAttack(@"../../../../unsized_64_v.csv");
        }

        static void WithRowTransformAttack(string filepath)
        {
            long attackUID = 134217728;
            double U = 1.0;
            double pos_val = 1.4901161193847663e-08;
            double neg_val = -1.490116119384765e-08;

            var arr_v_queryable = File.ReadAllLines(filepath)
                .AsQueryable();
            var dp_sum = new PINQueryable<string>(arr_v_queryable, new PINQAgentLogger(filepath))
                .Select(l => l.Split(','))
                .Select(terms => new Tuple<long, double>(Convert.ToInt64(terms[0]), Convert.ToDouble(terms[1])))
                .Select(tup => (tup.Item1 < attackUID) ? U :
                    (tup.Item1 == attackUID) ? U * tup.Item2 :
                    (tup.Item2 % 2 == 0) ? pos_val : neg_val)
                .NoisySum(100.0, v => v);

            Console.WriteLine("DP Sum: " + dp_sum);
        }

        static void WithoutRowTransform()
        {
            // 64 bit sum differs by 3!
            TestHarness.WithoutRowTransformAttack(@"../../../../unsized_64_trans_u.csv");
            TestHarness.WithoutRowTransformAttack(@"../../../../unsized_64_trans_v.csv");
        }

        static void WithoutRowTransformAttack(string filepath)
        {
            var arr_v_queryable = File.ReadAllLines(filepath).AsQueryable();
            var arr_v = new PINQueryable<string>(arr_v_queryable, new PINQAgentLogger(filepath));


            Console.WriteLine("DP Sum: " + arr_v.NoisySum(100.0, x => Convert.ToDouble(x)));
        }
    }
}
