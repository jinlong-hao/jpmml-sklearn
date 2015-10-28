/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.ensemble.gradient_boosting;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldUsageType;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningModel;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.MultipleModelMethodType;
import org.dmg.pmml.NumericPredictor;
import org.dmg.pmml.Output;
import org.dmg.pmml.RegressionModel;
import org.dmg.pmml.RegressionTable;
import org.dmg.pmml.Segmentation;
import org.dmg.pmml.TreeModel;
import org.jpmml.converter.PMMLUtil;
import sklearn.EstimatorUtil;
import sklearn.linear_model.RegressionModelUtil;
import sklearn.tree.DecisionTreeRegressor;
import sklearn.tree.TreeModelUtil;

public class GradientBoostingUtil {

	private GradientBoostingUtil(){
	}

	static
	public MiningModel encodeGradientBoosting(List<DecisionTreeRegressor> regressors, Number initialPrediction, Number learningRate, final List<DataField> dataFields, boolean standalone){
		List<Model> models = new ArrayList<>();

		FieldName sumField = FieldName.create("sum");

		{
			Function<DecisionTreeRegressor, TreeModel> function = new Function<DecisionTreeRegressor, TreeModel>(){

				@Override
				public TreeModel apply(DecisionTreeRegressor regressor){
					return TreeModelUtil.encodeTreeModel(regressor, MiningFunctionType.REGRESSION, dataFields, false);
				}
			};

			List<TreeModel> treeModels = Lists.transform(regressors, function);

			Segmentation segmentation = EstimatorUtil.encodeSegmentation(MultipleModelMethodType.SUM, treeModels, null);

			Output output = new Output()
				.addOutputFields(PMMLUtil.createPredictedField(sumField));

			MiningSchema miningSchema = PMMLUtil.createMiningSchema(null, dataFields.subList(1, dataFields.size()));

			MiningModel miningModel = new MiningModel(MiningFunctionType.REGRESSION, miningSchema)
				.setSegmentation(segmentation)
				.setOutput(output);

			models.add(miningModel);
		}

		DataField dataField = dataFields.get(0);

		{
			MiningField miningField = PMMLUtil.createMiningField(sumField);

			NumericPredictor numericPredictor = new NumericPredictor(miningField.getName(), learningRate.doubleValue());

			RegressionTable regressionTable = RegressionModelUtil.encodeRegressionTable(numericPredictor, initialPrediction);

			MiningSchema miningSchema;

			if(standalone){
				miningSchema = new MiningSchema()
					.addMiningFields(PMMLUtil.createMiningField(dataField.getName(), FieldUsageType.TARGET))
					.addMiningFields(miningField);
			} else

			{
				miningSchema = new MiningSchema()
					.addMiningFields(miningField);
			}

			RegressionModel regressionModel = new RegressionModel(MiningFunctionType.REGRESSION, miningSchema, null)
				.addRegressionTables(regressionTable);

			models.add(regressionModel);
		}

		Segmentation segmentation = EstimatorUtil.encodeSegmentation(MultipleModelMethodType.MODEL_CHAIN, models, null);

		MiningSchema miningSchema = PMMLUtil.createMiningSchema((standalone ? dataField : null), dataFields.subList(1, dataFields.size()));

		MiningModel miningModel = new MiningModel(MiningFunctionType.REGRESSION, miningSchema)
			.setSegmentation(segmentation);

		return miningModel;
	}
}