package ar.gov.justiciajujuy.sanpedro.inventario.web;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ar.gov.justiciajujuy.sanpedro.inventario.security.CredencialLocal;
import ar.gov.justiciajujuy.sanpedro.inventario.security.CredencialLocalRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.security.OrigenIdentidad;
import ar.gov.justiciajujuy.sanpedro.inventario.security.Rol;
import ar.gov.justiciajujuy.sanpedro.inventario.security.RolRepository;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioSistema;
import ar.gov.justiciajujuy.sanpedro.inventario.security.UsuarioSistemaRepository;

@Controller
public class SetupController {

	private final UsuarioSistemaRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final CredencialLocalRepository credencialLocalRepository;
	private final PasswordEncoder passwordEncoder;

	public SetupController(
			UsuarioSistemaRepository usuarioRepository,
			RolRepository rolRepository,
			CredencialLocalRepository credencialLocalRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.rolRepository = rolRepository;
		this.credencialLocalRepository = credencialLocalRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/setup")
	public String showSetupPage(Model model) {
		if (usuarioRepository.countAdministradores() > 0) {
			return "redirect:/login";
		}
		return "admin/setup";
	}

	@PostMapping("/setup")
	public String processSetup(
			@RequestParam String username,
			@RequestParam String nombreVisible,
			@RequestParam String password,
			Model model) {

		if (usuarioRepository.countAdministradores() > 0) {
			return "redirect:/login";
		}

		if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
			model.addAttribute("error", "El usuario y contraseña son obligatorios.");
			return "admin/setup";
		}

		try {
			Rol adminRol = rolRepository.findByCodigo("ADMINISTRADOR")
					.orElseThrow(() -> new IllegalStateException("El rol ADMINISTRADOR no existe en la base de datos."));

			UsuarioSistema admin = new UsuarioSistema(
					username.trim(),
					nombreVisible != null && !nombreVisible.trim().isEmpty() ? nombreVisible.trim() : "Administrador Principal",
					"Administracion del Sistema",
					OrigenIdentidad.LOCAL
			);
			admin.reemplazarRoles(Set.of(adminRol));
			
			usuarioRepository.save(admin);

			CredencialLocal credencial = new CredencialLocal(admin, passwordEncoder.encode(password), false);
			
			credencialLocalRepository.save(credencial);

			return "redirect:/login?setupSuccess=true";
		} catch (Exception e) {
			model.addAttribute("error", "Error al crear el administrador: " + e.getMessage());
			return "admin/setup";
		}
	}
}
