package nl.topicus.onderwijs.maven;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.enforcer.utils.ArtifactMatcher;
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

	public UnmanagedDependencyCollector(MavenProject project, DependencyNode root,
			List<String> excludes)
	{
		if (project.getDependencyManagement() == null)
			managedDependencies = Collections.emptySet();
		else
			managedDependencies =
				project.getDependencyManagement().getDependencies().stream()
					.map(Dependency::getManagementKey).collect(Collectors.toSet());
		this.root = root;
		this.excludes = excludes;
	}

	@Override
	public boolean visit(DependencyNode node)
	{
		if (node.equals(root))
			return true;

		try
		{
			if (matchesExclude(node))
				return true;
		}
		catch (InvalidVersionSpecificationException e)
		{
			storedException = new EnforcerRuleException("Invalid Version Range: ", e);
			return false;
		}

		if (!managedDependencies.contains(node.getArtifact().getDependencyConflictId()))
			unmanagedDependencies.add(node.toNodeString());
		return true;
	}

	private boolean matchesExclude(DependencyNode node) throws InvalidVersionSpecificationException
	{
		for (String curExclude : excludes)
		{
			ArtifactMatcher.Pattern am = new ArtifactMatcher.Pattern(curExclude);
			if (am.match(node.getArtifact()))
				return true;
		}
		return false;
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

	public List<String> getExcludes()
	{
		return excludes;
	}

	public void setExcludes(List<String> excludes)
	{
		this.excludes = excludes;
	}
}
