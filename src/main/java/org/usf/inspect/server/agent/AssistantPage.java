package org.usf.inspect.server.agent;

import java.util.Map;
import java.util.function.BiFunction;

public enum AssistantPage {
	DASHBOARD((provider, id) -> provider.resolveDashboard(id)),
	REQUEST_SEARCH((provider, id) -> provider.resolveRequestSearch(id)),
	SESSION_DETAIL_VIEW((provider, id) -> provider.resolveSessionDetailView(id)),
	REQUEST_DETAIL((provider, id) -> provider.resolveRequestDetail(id)),
	REQUEST_COMPARE((provider, id) -> provider.resolveRequestCompare(id)),
	SESSION_SEARCH((provider, id) -> provider.resolveSessionSearch(id)),
	SESSION_DETAIL((provider, id) -> provider.resolveSessionDetail(id)),
	SESSION_TREE((provider, id) -> provider.resolveSessionTree(id)),
	SESSION_COMPARE((provider, id) -> provider.resolveSessionCompare(id)),
	INSTANCE((provider, id) -> provider.resolveInstance(id)),
	ANALYTIC((provider, id) -> provider.resolveAnalytic(id)),
	ARCHITECTURE((provider, id) -> provider.resolveArchitecture(id)),
	SERVER_SUPERVISION((provider, id) -> provider.resolveServerSupervision(id)),
	CLIENT_SUPERVISION((provider, id) -> provider.resolveClientSupervision(id)),
	REQUEST_KPI((provider, id) -> provider.resolveRequestKpi(id)),
	SESSION_KPI((provider, id) -> provider.resolveSessionKpi(id)),
	UNKNOWN((provider, id) -> provider.resolveUnknown(id));

	private final BiFunction<InspectAgentContextProvider, String, Map<String, Object>> resolver;

	AssistantPage(BiFunction<InspectAgentContextProvider, String, Map<String, Object>> resolver) {
		this.resolver = resolver;
	}

	Map<String, Object> resolveContext(InspectAgentContextProvider provider, String id) {
		return resolver.apply(provider, id);
	}
}
