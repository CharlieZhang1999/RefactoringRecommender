package cmu.csdetector.util;

import cmu.csdetector.metrics.MethodMetricValueCollector;
import cmu.csdetector.metrics.TypeMetricValueCollector;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Type;

import java.util.List;

public class GenericCollector {

	public static void collectTypeMetricValues(Type type) {
		TypeMetricValueCollector collector = new TypeMetricValueCollector();
		collector.collect(type);
	}
	
	public static void collectTypeAndMethodsMetricValues(Type type) {
		TypeMetricValueCollector collector = new TypeMetricValueCollector();
		collector.collect(type);
		for (Method method : type.getMethods()) {
			MethodMetricValueCollector mColl = new MethodMetricValueCollector();
			mColl.collect(method);
		}
	}

	public static void collectAll(List<Type> types) {
		for (Type type: types) {
			collectTypeMetricValues(type);
			collectTypeAndMethodsMetricValues(type);

		}
	}
}
