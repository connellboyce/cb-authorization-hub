package com.connellboyce.authhub.service;

import com.connellboyce.authhub.model.dao.CBUser;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserService {
	UserDetails createUser(String username, String password, String email, String firstName, String lastName);
	CBUser getCBUserByUsername(String username);
}
