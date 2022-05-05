//
// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

// Package pbeamtest provides PrivacySpecs for testing Privacy on Beam pipelines
// without noise.
package pbeamtest

import (
	"math"

	"github.com/google/differential-privacy/go/v2/dpagg"
	"github.com/google/differential-privacy/privacy-on-beam/v2/internal/testoption"
	"github.com/google/differential-privacy/privacy-on-beam/v2/pbeam"
)

// NewPrivacySpecNoNoiseWithContributionBounding creates a new PrivacySpec with
// the specified privacy budget that adds no noise and keeps all partitions but
// still does contribution bounding.
//
// The epsilon and delta arguments are the total (ε,δ)-differential privacy
// budget for the pipeline. If there is only one aggregation, the entire budget
// will be used for this aggregation. Otherwise, the user must specify how the
// privacy budget is split across aggregations.
//
// This does NOT provide any privacy protections, so should only be used in
// test code in order to avoid dealing with random noise.
func NewPrivacySpecNoNoiseWithContributionBounding(epsilon, delta float64) *pbeam.PrivacySpec {
	return pbeam.NewPrivacySpec(epsilon, delta, testoption.EnableNoNoiseWithContributionBounding{})
}

// NewPrivacySpecNoNoiseWithoutContributionBounding creates a new PrivacySpec with
// the specified privacy budget that adds no noise, keeps all partitions and disables
// contribution bounding (both per-partition and cross-partition). The contribution
// bounding parameters should still be specified for all the pbeam aggregations
// in the pipeline since they will be validated.
//
// The epsilon and delta arguments are the total (ε,δ)-differential privacy
// budget for the pipeline. If there is only one aggregation, the entire budget
// will be used for this aggregation. Otherwise, the user must specify how the
// privacy budget is split across aggregations.
//
// This does NOT provide any privacy protections, so should only be used in
// test code in order to avoid dealing with random noise.
func NewPrivacySpecNoNoiseWithoutContributionBounding(epsilon, delta float64) *pbeam.PrivacySpec {
	return pbeam.NewPrivacySpec(epsilon, delta, testoption.EnableNoNoiseWithoutContributionBounding{})
}

// QuantilesTolerance returns a tolerance t such that the output of QuantilesPerKey is
// within t of the exact result for given MinValue and MaxValue parameters of
// QuantilesParams when pbeamtest is used.
//
// Due to the implementation details of Quantiles, it has an inherent (non-DP) noise. So,
// even when we disable DP noise, the results will be still slightly noisy.
func QuantilesTolerance(MinValue, MaxValue float64) float64 {
	return (MaxValue - MinValue) / math.Pow(float64(dpagg.DefaultBranchingFactor), float64(dpagg.DefaultTreeHeight))
}
