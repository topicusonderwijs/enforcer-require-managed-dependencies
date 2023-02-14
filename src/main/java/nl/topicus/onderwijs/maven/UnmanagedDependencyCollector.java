package nl.topicus.onderwijs.maven;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rules.utils.ArtifactMatcher;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

public class UnmanagedDependencyCollector implements DependencyNodeVisitor
{
	private Set<String> managedDependencies;

	private Set<String> unmanagedDependencies = new TreeSet<>();

	private DependencyNode root;

	private List<String> excludes;

	private EnforcerRuleException storedException;

	private List<String> includedScopes;

	public UnmanagedDependencyCollector(MavenProject project, DependencyNode root,
			List<String> excludes, List<String> includedScopes)
	{
		if (project.getDependencyManagement() == null)
			managedDependencies = Collections.emptySet();
		else
			managedDependencies = project.getDependencyManagement()
				.getDependencies()
				.stream()
				.map(Dependency::getManagementKey)
				.collect(Collectors.toSet());
		this.root = root;
		this.excludes = excludes;
		this.includedScopes = includedScopes;
	}

	@Override
	public boolean visit(DependencyNode node)
	{
		if (node.equals(root))
			return true;

		if (matchesExclude(node) || !scopeIncluded(node))
			return true;

		if (!managedDependencies.contains(node.getArtifact().getDependencyConflictId()))
			unmanagedDependencies.add(node.toNodeString());
		return true;
	}

	private boolean matchesExclude(DependencyNode node)
	{
		for (String curExclude : excludes)
		{
			ArtifactMatcher.Pattern am = new ArtifactMatcher.Pattern(curExclude);
			if (am.match(node.getArtifact()))
				return true;
		}
		return false;
	}

	private boolean scopeIncluded(DependencyNode node)
	{
		return includedScopes.contains(node.getArtifact().getScope());
	}

	@Override
	public boolean endVisit(DependencyNode node)
	{
		return true;
	}

	public Set<String> getUnmanagedDependencies()
	{
		return unmanagedDependencies;
	}

	public EnforcerRuleException getStoredException()
	{
		return storedException;
	}
}
