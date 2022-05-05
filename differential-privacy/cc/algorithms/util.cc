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

#include "algorithms/util.h"

#include <cmath>
#include <cstdlib>
#include <vector>

#include "absl/status/status.h"
#include "base/statusor.h"
#include "base/status_macros.h"

namespace differential_privacy {

double DefaultEpsilon() { return std::log(3); }

double GetNextPowerOfTwo(double n) { return std::pow(2.0, ceil(log2(n))); }

double InverseErrorFunction(double x) {
  double LESS_THAN_FIVE_CONSTANTS[] = {
      0.0000000281022636, 0.000000343273939, -0.0000035233877,
      -0.00000439150654,  0.00021858087,     -0.00125372503,
      -0.00417768164,     0.246640727,       1.50140941};
  double GREATER_THAN_FIVE_CONSTANTS[] = {
      -0.000200214257, 0.000100950558, 0.00134934322,
      -0.00367342844,  0.00573950773,  -0.0076224613,
      0.00943887047,   1.00167406,     2.83297682};

  double constantArray[9];
  double w = -std::log((1 - x) * (1 + x));
  double ans = 0;

  if (std::abs(x) == 1) {
    return x * std::numeric_limits<double>::infinity();
  }

  if (w < 5) {
    w = w - 2.5;
    std::copy(std::begin(LESS_THAN_FIVE_CONSTANTS),
              std::end(LESS_THAN_FIVE_CONSTANTS), std::begin(constantArray));
  } else {
    w = std::sqrt(w) - 3;
    std::copy(std::begin(GREATER_THAN_FIVE_CONSTANTS),
              std::end(GREATER_THAN_FIVE_CONSTANTS), std::begin(constantArray));
  }

  for (int i = 0; i < 9; i++) {
    double coefficient = constantArray[i];
    ans = coefficient + ans * w;
  }

  return ans * x;
}

base::StatusOr<double> Qnorm(double p, double mu, double sigma) {
  if (p <= 0.0 || p >= 1.0) {
    return absl::InvalidArgumentError(
        "Probability must be between 0 and 1, exclusive.");
  }
  double t = std::sqrt(-2.0 * std::log(std::min(p, 1.0 - p)));
  std::vector<double> c = {2.515517, 0.802853, 0.010328};
  std::vector<double> d = {1.432788, 0.189269, 0.001308};
  double normalized = t - ((c[2] * t + c[1]) * t + c[0]) /
                              (((d[2] * t + d[1]) * t + d[0]) * t + 1.0);
  if (p < .5) {
    normalized *= -1;
  }
  return normalized * sigma + mu;
}

double RoundToNearestDoubleMultiple(double n, double base) {
  if (base == 0.0) return n;
  double remainder = fmod(n, base);
  if (std::abs(remainder) > base / 2) {
    return n - remainder + sign(remainder) * base;
  }
  if (std::abs(remainder) == base / 2) {
    return n + base / 2;
  }
  return n - remainder;
}

int64_t RoundToNearestInt64Multiple(int64_t n, int64_t base) {
  if (base == 0) return n;
  int64_t remainder = n % base;
  if (std::abs(remainder) > base / 2.0) {
    return n - remainder + sign(remainder) * base;
  }
  if (std::abs(remainder) * 2 == base) {
    return n + base / 2;
  }
  return n - remainder;
}

absl::Status ValidateIsSet(absl::optional<double> opt, absl::string_view name,
                           absl::StatusCode error_code) {
  if (!opt.has_value()) {
    return absl::InvalidArgumentError(absl::StrCat(name, " must be set."));
  }
  double d = opt.value();
  if (std::isnan(d)) {
    return absl::Status(
        error_code,
        absl::StrCat(name, " must be a valid numeric value, but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsPositive(absl::optional<double> opt,
                                absl::string_view name,
                                absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();
  if (d <= 0) {
    return absl::Status(
        error_code, absl::StrCat(name, " must be positive, but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsNonNegative(absl::optional<double> opt,
                                   absl::string_view name,
                                   absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();
  if (d < 0) {
    return absl::Status(
        error_code,
        absl::StrCat(name, " must be non-negative, but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsFinite(absl::optional<double> opt,
                              absl::string_view name,
                              absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();
  if (!std::isfinite(d)) {
    return absl::Status(error_code,
                        absl::StrCat(name, " must be finite, but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsFiniteAndPositive(absl::optional<double> opt,
                                         absl::string_view name,
                                         absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();
  if (d <= 0 || !std::isfinite(d)) {
    return absl::Status(
        error_code,
        absl::StrCat(name, " must be finite and positive, but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsFiniteAndNonNegative(absl::optional<double> opt,
                                            absl::string_view name,
                                            absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();
  if (d < 0 || !std::isfinite(d)) {
    return absl::Status(
        error_code,
        absl::StrCat(name, " must be finite and non-negative, but is ", d,
                     "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateIsInInclusiveInterval(absl::optional<double> opt,
                                           double lower_bound,
                                           double upper_bound,
                                           absl::string_view name,
                                           absl::StatusCode error_code) {
  return ValidateIsInInterval(opt, lower_bound, upper_bound, true, true, name,
                              error_code);
}

absl::Status ValidateIsInExclusiveInterval(absl::optional<double> opt,
                                           double lower_bound,
                                           double upper_bound,
                                           absl::string_view name,
                                           absl::StatusCode error_code) {
  return ValidateIsInInterval(opt, lower_bound, upper_bound, false, false, name,
                              error_code);
}

absl::Status ValidateIsLesserThan(absl::optional<double> opt,
                                  double upper_bound, absl::string_view name,
                                  absl::StatusCode error_code) {
  double lower_bound = -std::numeric_limits<double>::infinity();
  bool include_lower = lower_bound != upper_bound;

  absl::Status result = ValidateIsInInterval(
      opt, lower_bound, upper_bound, include_lower, false, name, error_code);
  if (result.ok()) {
    return absl::OkStatus();
  } else {
    return absl::Status(error_code,
                        absl::StrCat(name, " must be lesser than ", upper_bound,
                                     ", but is ", opt.value(), "."));
  }
}

absl::Status ValidateIsLesserThanOrEqualTo(absl::optional<double> opt,
                                           double upper_bound,
                                           absl::string_view name,
                                           absl::StatusCode error_code) {
  absl::Status result =
      ValidateIsInInterval(opt, -std::numeric_limits<double>::infinity(),
                           upper_bound, true, true, name, error_code);
  if (result.ok()) {
    return absl::OkStatus();
  } else {
    return absl::Status(
        error_code, absl::StrCat(name, " must be lesser than or equal to ",
                                 upper_bound, ", but is ", opt.value(), "."));
  }
}

absl::Status ValidateIsGreaterThan(absl::optional<double> opt,
                                   double lower_bound, absl::string_view name,
                                   absl::StatusCode error_code) {
  double upper_bound = std::numeric_limits<double>::infinity();
  bool include_upper = lower_bound != upper_bound;

  absl::Status result = ValidateIsInInterval(
      opt, lower_bound, upper_bound, false, include_upper, name, error_code);
  if (result.ok()) {
    return absl::OkStatus();
  } else {
    return absl::Status(
        error_code, absl::StrCat(name, " must be greater than ", lower_bound,
                                 ", but is ", opt.value(), "."));
  }
}

absl::Status ValidateIsGreaterThanOrEqualTo(absl::optional<double> opt,
                                            double lower_bound,
                                            absl::string_view name,
                                            absl::StatusCode error_code) {
  absl::Status result = ValidateIsInInterval(
      opt, lower_bound, std::numeric_limits<double>::infinity(), true, true,
      name, error_code);
  if (result.ok()) {
    return absl::OkStatus();
  } else {
    return absl::Status(
        error_code, absl::StrCat(name, " must be greater than or equal to ",
                                 lower_bound, ", but is ", opt.value(), "."));
  }
}

absl::Status ValidateIsInInterval(absl::optional<double> opt,
                                  double lower_bound, double upper_bound,
                                  bool include_lower, bool include_upper,
                                  absl::string_view name,
                                  absl::StatusCode error_code) {
  RETURN_IF_ERROR(ValidateIsSet(opt, name, error_code));
  double d = opt.value();

  if (lower_bound == upper_bound && upper_bound == d &&
      (include_lower || include_upper)) {
    return absl::OkStatus();
  }
  bool d_is_outside_lower_bound =
      include_lower ? d < lower_bound : d <= lower_bound;
  bool d_is_outside_upper_bound =
      include_upper ? d > upper_bound : d >= upper_bound;
  if (d_is_outside_lower_bound || d_is_outside_upper_bound) {
    std::string left_bracket = include_lower ? "[" : "(";
    std::string right_bracket = include_upper ? "]" : ")";
    std::string inclusivity = " ";
    if (include_lower && include_upper) {
      inclusivity = " inclusive ";
    } else if (!include_lower && !include_upper) {
      inclusivity = " exclusive ";
    }

    return absl::Status(
        error_code,
        absl::StrCat(name, " must be in the", inclusivity, "interval ",
                     left_bracket, lower_bound, ",", upper_bound, right_bracket,
                     ", but is ", d, "."));
  }
  return absl::OkStatus();
}

absl::Status ValidateEpsilon(absl::optional<double> epsilon) {
  return ValidateIsFiniteAndPositive(epsilon, "Epsilon");
}

absl::Status ValidateDelta(absl::optional<double> delta) {
  return ValidateIsInInclusiveInterval(delta, 0, 1, "Delta");
}

absl::Status ValidateMaxPartitionsContributed(
    absl::optional<double> max_partitions_contributed) {
  return ValidateIsPositive(max_partitions_contributed,
                            "Maximum number of partitoins that can be "
                            "contributed to (i.e., L0 sensitivity)");
}

absl::Status ValidateMaxContributionsPerPartition(
    absl::optional<double> max_contributions_per_partition) {
  return ValidateIsPositive(max_contributions_per_partition,
                            "Maximum number of contributions per partition");
}

}  // namespace differential_privacy
