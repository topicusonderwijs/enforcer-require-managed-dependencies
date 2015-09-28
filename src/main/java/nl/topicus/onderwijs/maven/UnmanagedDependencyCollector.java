package nl.topicus.onderwijs.maven;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

public class UnmanagedDependencyCollector implements DependencyNodeVisitor {
	private Set<String> managedDependencies;
	private Set<String> unmanagedDependencies = new TreeSet<>();

	public UnmanagedDependencyCollector(MavenProject project) {
		managedDependencies = project.getDependencyManagement()
				.getDependencies().stream().map(Dependency::getManagementKey)
				.collect(Collectors.toSet());
	}

	@Override
	public boolean visit(DependencyNode node) {
		if (!managedDependencies.contains(node.getArtifact()
				.getDependencyConflictId()))
			unmanagedDependencies.add(node.toNodeString());
		return true;
	}

	@Override
	public boolean endVisit(DependencyNode node) {
		return true;
	}

	public Set<String> getUnmanagedDependencies() {
		return unmanagedDependencies;
	}
}