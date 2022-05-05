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
"""Tests for central_privacy_utils."""

from unittest import mock

from absl.testing import absltest
from absl.testing import parameterized
import numpy as np
from scipy import stats

from clustering import central_privacy_utils
from clustering import clustering_params
from clustering import test_utils
from clustering.central_privacy_utils import AveragePrivacyParam
from clustering.central_privacy_utils import CountPrivacyParam


class CentralPrivacyUtilsTest(parameterized.TestCase):

  def test_average_privacy_param(self):
    clustering_param = test_utils.get_test_clustering_param(
        epsilon=10, delta=1e-2, frac_sum=0.7, frac_group_count=0.3, radius=4.3)
    average_privacy_param = AveragePrivacyParam.from_clustering_param(
        clustering_param)
    self.assertEqual(average_privacy_param.epsilon, 7.0)
    self.assertEqual(average_privacy_param.delta, 1e-2)
    self.assertEqual(average_privacy_param.sensitivity, 4.3)

  @parameterized.named_parameters(
      ('basic', [[1, 2, 1], [0.4, 0.2, 0.8], [3, 0, 3]], [3.1, 5.55, 0.2]),
      ('empty', [], [2, 5, -1]))
  @mock.patch.object(
      np.random, 'normal', return_value=np.array([8, 20, -4]), autospec=True)
  def test_get_private_average(self, nonprivate_points, expected_center,
                               mock_normal_fn):
    private_count = 4
    average_privacy_param = AveragePrivacyParam(
        epsilon=7, delta=1e-2, sensitivity=4.3)

    result = central_privacy_utils.get_private_average(
        nonprivate_points, private_count, average_privacy_param, dim=3)
    self.assertSequenceAlmostEqual(result, expected_center)
    mock_normal_fn.assert_called_once()
    self.assertEqual(mock_normal_fn.call_args[1]['size'], 3)
    self.assertAlmostEqual(
        mock_normal_fn.call_args[1]['scale'], 1.927768, delta=1e-5)

  def test_get_private_average_error(self):
    nonprivate_points = [[1, 2, 1], [0.4, 0.2, 0.8], [3, 0, 3]]
    average_privacy_param = AveragePrivacyParam(
        epsilon=7, delta=1e-2, sensitivity=4.3)

    with self.assertRaises(ValueError):
      central_privacy_utils.get_private_average(
          nonprivate_points, 0, average_privacy_param, dim=3)
    with self.assertRaises(ValueError):
      central_privacy_utils.get_private_average(
          nonprivate_points, -2, average_privacy_param, dim=3)

  def test_get_private_average_infinite_eps(self):
    nonprivate_points = [[1, 2, 1], [0.2, 0.1, 0.8], [3, 0, 3]]
    private_count = 3
    expected_center = [1.4, 0.7, 1.6]
    average_privacy_param = AveragePrivacyParam(
        epsilon=np.inf, delta=1e-2, sensitivity=4.3)
    self.assertSequenceAlmostEqual(
        central_privacy_utils.get_private_average(
            nonprivate_points, private_count, average_privacy_param, dim=3),
        expected_center)

  def test_private_count_param(self):
    privacy_param = clustering_params.DifferentialPrivacyParam(
        epsilon=10, delta=1e-2)
    privacy_budget_split = clustering_params.PrivacyBudgetSplit(
        frac_sum=0.2, frac_group_count=0.8)
    max_tree_depth = 3
    count_privacy_param = CountPrivacyParam.compute_group_count_privacy_param(
        privacy_param, privacy_budget_split, max_tree_depth)
    self.assertEqual(count_privacy_param.epsilon, 2.0)
    self.assertEqual(count_privacy_param.delta, 1e-2)

  def test_private_count_param_from_clustering_param(self):
    clustering_param = test_utils.get_test_clustering_param(
        epsilon=10, delta=1e-2, frac_sum=0.2, frac_group_count=0.8, max_depth=3)
    count_privacy_param = CountPrivacyParam.from_clustering_param(
        clustering_param)
    self.assertEqual(count_privacy_param.epsilon, 2.0)
    self.assertEqual(count_privacy_param.delta, 1e-2)

  @parameterized.named_parameters(('basic', 10, 70), ('not_clip', -80, -20))
  @mock.patch.object(stats.dlaplace, 'rvs', autospec=True)
  def test_get_private_count(self, dlaplace_noise, expected_private_count,
                             mock_dlaplace_fn):
    mock_dlaplace_fn.return_value = dlaplace_noise

    nonprivate_count = 60
    count_privacy_param = CountPrivacyParam(epsilon=2.0, delta=1e-2)

    result = central_privacy_utils.get_private_count(nonprivate_count,
                                                     count_privacy_param)
    self.assertEqual(result, expected_private_count)
    mock_dlaplace_fn.assert_called_once_with(2)

  def test_get_private_count_infinite_eps(self):
    nonprivate_count = 60
    count_privacy_param = CountPrivacyParam(epsilon=np.inf, delta=1e-2)
    self.assertEqual(
        central_privacy_utils.get_private_count(nonprivate_count,
                                                count_privacy_param),
        nonprivate_count)


if __name__ == '__main__':
  absltest.main()
