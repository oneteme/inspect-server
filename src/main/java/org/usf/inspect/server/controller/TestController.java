package org.usf.inspect.server.controller;

import static org.usf.inspect.server.config.TraceApiDatabase.INSPECT;
import static org.usf.jquery.web.mvc.MvcExecutors.executor;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.usf.jquery.core.DynamicModel;
import org.usf.jquery.core.QueryComposer;
import org.usf.jquery.web.QueryRequest;
import org.usf.jquery.web.mvc.MvcExecutors;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequiredArgsConstructor
public class TestController {


	@GetMapping("/")
	public Object chartPage(
			@QueryRequest(view = "database_stage", ignoreParameters = "view") QueryComposer req, 
			@RequestParam Optional<String> view, HttpServletResponse response) {
		
		return INSPECT.run(req, executor(response, view));
	}

	@GetMapping("/callback/{id}")
	public List<DynamicModel> callback(@PathVariable String id, HttpServletResponse response) {
		
		return MvcExecutors.callback(id, response);
	}

	@GetMapping("db/stg")
	public Object databaseStage(
			@QueryRequest(view = "database_stage", ignoreParameters = "view") QueryComposer req, 
			@RequestParam Optional<String> view, HttpServletResponse response)   {

		return INSPECT.run(req, executor(response, view));
	}

	@GetMapping("db/req")
	public Object databaseRequest(
			@QueryRequest(view = "database_request", ignoreParameters = "view") QueryComposer req,  
			@RequestParam Optional<String> view, HttpServletResponse response)  {

		return INSPECT.run(req, executor(response, view));
	}
}
