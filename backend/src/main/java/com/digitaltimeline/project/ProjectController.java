package com.digitaltimeline.project;
import com.digitaltimeline.project.ProjectRepository.ProjectDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/projects")
public class ProjectController {
  public record CreateProject(@NotBlank String name, String description) {}
  private final ProjectRepository projects;
  public ProjectController(ProjectRepository p) { this.projects = p; }
  @GetMapping List<ProjectDto> list(@AuthenticationPrincipal UUID u) { return projects.list(u); }
  @GetMapping("/{id}") ProjectDto get(@AuthenticationPrincipal UUID u, @PathVariable UUID id) { return projects.get(u, id); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  ProjectDto create(@AuthenticationPrincipal UUID u, @Valid @RequestBody CreateProject r) { return projects.create(u, r.name().trim(), r.description()); }
}
