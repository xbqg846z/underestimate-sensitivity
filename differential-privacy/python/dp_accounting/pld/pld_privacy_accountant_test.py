# Copyright 2021 Google LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Tests for pld_privacy_accountant."""

import math

from absl.testing import absltest
from absl.testing import parameterized

from dp_accounting import dp_event
from dp_accounting import privacy_accountant_test
from dp_accounting.pld import pld_privacy_accountant


class PldPrivacyAccountantTest(privacy_accountant_test.PrivacyAccountantTest,
                               parameterized.TestCase):

  def _make_test_accountants(self):
    return [pld_privacy_accountant.PLDAccountant()]

  @parameterized.parameters(
      dp_event.GaussianDpEvent(1.0),
      dp_event.SelfComposedDpEvent(dp_event.GaussianDpEvent(1.0), 6),
      dp_event.ComposedDpEvent(
          [dp_event.GaussianDpEvent(1.0),
           dp_event.GaussianDpEvent(2.0)]),
      dp_event.PoissonSampledDpEvent(0.1, dp_event.GaussianDpEvent(1.0)),
      dp_event.ComposedDpEvent([
          dp_event.PoissonSampledDpEvent(0.1, dp_event.GaussianDpEvent(1.0)),
          dp_event.GaussianDpEvent(2.0)
      ]))
  def test_supports_gaussian(self, event):
    pld_accountant = pld_privacy_accountant.PLDAccountant()
    self.assertTrue(pld_accountant.supports(event))

  @parameterized.parameters(0, -1)
  def test_non_positive_composition_value_error(self, count):
    event = dp_event.GaussianDpEvent(1.0)
    accountant = pld_privacy_accountant.PLDAccountant()
    with self.assertRaises(ValueError):
      accountant.compose(event, count)

  def test_gaussian_basic(self):
    gaussian_event = dp_event.GaussianDpEvent(noise_multiplier=math.sqrt(3))
    accountant = pld_privacy_accountant.PLDAccountant()
    accountant.compose(gaussian_event, 1)
    accountant.compose(gaussian_event, 2)

    exact_epsilon = 1
    exact_delta = 0.126936
    self.assertAlmostEqual(
        accountant.get_delta(exact_epsilon), exact_delta, delta=1e-3)
    self.assertAlmostEqual(
        accountant.get_epsilon(exact_delta), exact_epsilon, delta=1e-3)

  def test_poisson_subsampled_gaussian(self):
    subsampled_gaussian_event = dp_event.PoissonSampledDpEvent(
        0.2, dp_event.GaussianDpEvent(noise_multiplier=0.5))
    accountant = pld_privacy_accountant.PLDAccountant()
    accountant.compose(subsampled_gaussian_event, 1)
    accountant.compose(subsampled_gaussian_event, 2)

    exact_epsilon = 1
    expected_delta = 0.15594
    self.assertAlmostEqual(
        accountant.get_delta(exact_epsilon), expected_delta, delta=1e-3)
    self.assertAlmostEqual(
        accountant.get_epsilon(expected_delta), exact_epsilon, delta=1e-3)

  def test_self_composed_subsampled_gaussian(self):
    event = dp_event.SelfComposedDpEvent(
        dp_event.PoissonSampledDpEvent(0.2, dp_event.GaussianDpEvent(0.5)), 3)
    accountant = pld_privacy_accountant.PLDAccountant()
    accountant.compose(event)

    exact_epsilon = 1
    expected_delta = 0.15594
    self.assertAlmostEqual(
        accountant.get_delta(exact_epsilon), expected_delta, delta=1e-3)
    self.assertAlmostEqual(
        accountant.get_epsilon(expected_delta), exact_epsilon, delta=1e-3)


if __name__ == '__main__':
  absltest.main()
