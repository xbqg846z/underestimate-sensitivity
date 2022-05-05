package main

import (
	"github.com/apache/beam/sdks/v2/go/pkg/beam"
	"github.com/google/differential-privacy/privacy-on-beam/v2/pbeam"
)

func init() {
	beam.RegisterFunction(extractTuple)
}

func extractTuple(v pair) (int, float64) {
	return 0, v.V
}

func privateSum(s beam.Scope, col beam.PCollection) beam.PCollection {
	s = s.Scope("PrivateSum")
	epsilon := 1e20
	// Create a Privacy Spec and convert col into a PrivatePCollection.
	spec := pbeam.NewPrivacySpec(epsilon /* delta */, 0)
	pCol := pbeam.MakePrivateFromStruct(s, col, spec, "K")

	partitions := beam.CreateList(s, [1]int{0})

	values := pbeam.ParDo(s, extractTuple, pCol)
	revenues := pbeam.SumPerKey(s, values, pbeam.SumParams{
		MaxPartitionsContributed: 1, // Each individual contributes at most one record
		MinValue:                 -1,
		MaxValue:                 1,
		PublicPartitions:         partitions,
	})
	return revenues
}
