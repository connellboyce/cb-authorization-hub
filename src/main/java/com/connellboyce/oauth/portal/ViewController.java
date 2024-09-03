package com.connellboyce.oauth.portal;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
	@GetMapping("/pages/login")
	public String loginPage() {
		return "pages/login";
	}
}
