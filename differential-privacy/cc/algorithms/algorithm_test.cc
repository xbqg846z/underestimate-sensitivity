//
// Copyright 2019 Google LLC
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

#include "algorithms/algorithm.h"

#include <forward_list>
#include <list>
#include <vector>

#include "base/testing/status_matchers.h"
#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "base/statusor.h"

namespace differential_privacy {
namespace {

using ::testing::DoubleNear;
using ::testing::HasSubstr;
using ::differential_privacy::base::testing::StatusIs;

const double kTestPrecision = 1e-5;

template <typename T>
class TestAlgorithm : public Algorithm<T> {
 public:
  class Builder : public AlgorithmBuilder<T, TestAlgorithm<T>, Builder> {
    using AlgorithmBuilder =
        differential_privacy::AlgorithmBuilder<T, TestAlgorithm<T>, Builder>;

   public:
    Builder() : AlgorithmBuilder() {}

   private:
    base::StatusOr<std::unique_ptr<TestAlgorithm<T>>> BuildAlgorithm()
        override {
      return absl::WrapUnique(
          new TestAlgorithm(AlgorithmBuilder::GetEpsilon().value()));
    }
  };

  TestAlgorithm() : Algorithm<T>(1.0) {}
  TestAlgorithm(double epsilon) : Algorithm<T>(epsilon) {}
  void AddEntry(const T& t) override {}
  Summary Serialize() const override { return Summary(); }
  absl::Status Merge(const Summary& summary) override {
    return absl::OkStatus();
  }
  int64_t MemoryUsed() override { return sizeof(TestAlgorithm<T>); }

 protected:
  base::StatusOr<Output> GenerateResult(double noise_interval_level) override {
    Output output;
    ConfidenceInterval ci;
    ci.set_confidence_level(noise_interval_level);
    return MakeOutput("Data", ci);
  }
  void ResetState() override {}
};

TEST(AlgorithmTest, PartialResultPassesConfidenceLevel) {
  TestAlgorithm<double> alg;
  const double level = .9;
  base::StatusOr<Output> result = alg.PartialResult(level);
  ASSERT_OK(result);
  EXPECT_EQ(GetNoiseConfidenceInterval(*result).confidence_level(), level);

  // Although the ErrorReport.noise_confidence_interval is deprecated, we still
  // keep it updated for a more seamless transition for existing clients. After
  // some time, we should no longer use ErrorReport.noise_confidence_interval.
  // But for now, test to make sure ErrorReport.noise_confidence_interval is
  // being set.
  EXPECT_EQ(
      result->error_report().noise_confidence_interval().confidence_level(),
      level);
}

TEST(AlgorithmTest, RepeatedResultsFail) {
  TestAlgorithm<double> alg;
  EXPECT_OK(alg.PartialResult());
  EXPECT_THAT(alg.PartialResult(),
              StatusIs(absl::StatusCode::kInvalidArgument,
                       HasSubstr("can only produce results once")));
}

TEST(AlgorithmTest, Reset) {
  TestAlgorithm<double> alg;
  ASSERT_OK(alg.PartialResult());
  alg.Reset();
  EXPECT_OK(alg.PartialResult());
}

TEST(AlgorithmTest, InvalidEpsilon) {
  EXPECT_DEATH(TestAlgorithm<double> alg(-1.0), "Check failed: epsilon > 0.0");
  EXPECT_DEATH(
      TestAlgorithm<double> alg(std::numeric_limits<double>::quiet_NaN()),
      "Check failed: epsilon > 0.0");
  EXPECT_DEATH(
      TestAlgorithm<double> alg(std::numeric_limits<double>::infinity()),
      "Check failed: epsilon != std::numeric_limits<double>::infinity.*");
}

TEST(AlgorithmBuilderTest, InvalidEpsilonFailsBuild) {
  TestAlgorithm<double>::Builder builder;

  EXPECT_THAT(builder.SetEpsilon(-1).Build(),
              StatusIs(absl::StatusCode::kInvalidArgument,
                       HasSubstr("Epsilon must be finite and positive")));

  EXPECT_THAT(
      builder.SetEpsilon(std::numeric_limits<double>::quiet_NaN()).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Epsilon must be a valid numeric value")));

  EXPECT_THAT(
      builder.SetEpsilon(std::numeric_limits<double>::infinity()).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Epsilon must be finite")));
}

TEST(AlgorithmBuilderTest, InvalidDeltaFailsBuild) {
  TestAlgorithm<double>::Builder builder;

  EXPECT_THAT(
      builder.SetDelta(-0.1).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Delta must be in the inclusive interval [0,1]")));

  EXPECT_THAT(
      builder.SetDelta(1.1).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Delta must be in the inclusive interval [0,1]")));

  EXPECT_THAT(
      builder.SetDelta(std::numeric_limits<double>::quiet_NaN()).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Delta must be a valid numeric value")));

  EXPECT_THAT(
      builder.SetDelta(std::numeric_limits<double>::infinity()).Build(),
      StatusIs(absl::StatusCode::kInvalidArgument,
               HasSubstr("Delta must be in the inclusive interval [0,1]")));
}

TEST(AlgorithmBuilderTest, InvalidL0SensitivityFailsBuild) {
  TestAlgorithm<double>::Builder builder;

  EXPECT_THAT(
      builder.SetMaxPartitionsContributed(-1).Build(),
      StatusIs(
          absl::StatusCode::kInvalidArgument,
          HasSubstr("Maximum number of partitions that can be "
                    "contributed to (i.e., L0 sensitivity) must be positive")));

  EXPECT_THAT(
      builder.SetMaxPartitionsContributed(0).Build(),
      StatusIs(
          absl::StatusCode::kInvalidArgument,
          HasSubstr("Maximum number of partitions that can be "
                    "contributed to (i.e., L0 sensitivity) must be positive")));
}

TEST(AlgorithmBuilderTest, InvalidMaxContributionsPerPartitionFailsBuild) {
  TestAlgorithm<double>::Builder builder;

  EXPECT_THAT(builder.SetMaxContributionsPerPartition(-1).Build(),
              StatusIs(absl::StatusCode::kInvalidArgument,
                       HasSubstr("Maximum number of contributions per "
                                 "partition must be positive")));

  EXPECT_THAT(builder.SetMaxContributionsPerPartition(0).Build(),
              StatusIs(absl::StatusCode::kInvalidArgument,
                       HasSubstr("Maximum number of contributions per "
                                 "partition must be positive")));
}

}  // namespace
}  // namespace differential_privacy
