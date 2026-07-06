package org.usf.inspect.server.controller;

import static java.util.UUID.fromString;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;
import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.jquery.mvc.QueryExtension.Modifier.REJECT;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.mapper.InspectMappers;
import org.usf.inspect.server.model.MainSession;
import org.usf.inspect.server.model.RequestType;
import org.usf.inspect.server.repo.InspectStore;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.mvc.MvcRequest;
import org.usf.jquery.mvc.QueryExtension;
import org.usf.jquery.mvc.QueryExtension.Modifier;
import org.usf.jquery.mvc.QueryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@Validated
@RestController
@RequestMapping(value = "v3/query", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RequestControllerV5 {

//    private final RequestService requestService;
	private final ObjectMapper mapper;

	@GetMapping(value = "instance/{idInstance}", produces = APPLICATION_JSON_VALUE)
	@QueryExtension(select = REJECT)
	@QueryTemplate(dataset = "instance", view = "instanceMapper", select = "app_name,version,address,environement,os,re,user,type,start,collector,branch,hash,end,resource,configuration,id")
	public Object fetchInstance(MvcRequest mvc, HttpServletResponse res, @PathVariable String idInstance) {
		var store = (InspectStore) mvc.getStore();
		mvc.getComposer().criteria(store.instance().id().eq(fromString(idInstance))); // UUID

		return ok().cacheControl(maxAge(1, HOURS)).body(mvc.execute());
	}
	
	@GetMapping(value = "session/main/{idSession}", produces = APPLICATION_JSON_VALUE)
	@QueryExtension(select = REJECT)
	@QueryTemplate(dataset = "main_session", view = "mainSessionMapper", select = "id,name,start,end,type,location,thread,err_type,err_msg,stacktrace,mask,user,instance_env")
	public Object fetchJDBC(MvcRequest mvc, HttpServletResponse res, @PathVariable String idSession) {
		var store = (InspectStore) mvc.getStore();
		mvc.getComposer().criteria(store.mainSession().id().eq(fromString(idSession))); // UUID
		
		return Optional.ofNullable(mvc.execute())
                .map(o -> ok().body(o))
                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
	}
	
//    @GetMapping(value = "session/main/{idSession}", produces = APPLICATION_JSON_VALUE)
//    public ResponseEntity<MainSession> getMainSession(
//            @QueryRequestFilter(
//                view = "main_session",
//                column = "id,name,start,end,type,location,thread,err_type,err_msg,stacktrace,mask,user,instance_env") QueryComposer request,
//            @PathVariable String idSession) throws SQLException {
//        return Optional.ofNullable(INSPECT.execute(request.filters(column("id_ses").eq(fromString(idSession))), InspectMappers.createBaseMainSession(mapper)))
//                .map(o -> ok().body(o))
//                .orElseGet(() -> status(HttpStatus.NOT_FOUND).body(null));
//    }
}
