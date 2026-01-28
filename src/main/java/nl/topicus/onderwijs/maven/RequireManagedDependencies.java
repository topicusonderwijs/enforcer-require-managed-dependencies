package nl.topicus.onderwijs.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

@Named("requireManagedDependencies")
public class RequireManagedDependencies extends AbstractEnforcerRule
{
	@Inject
	private MavenProject project;

	@Inject
	private MavenSession session;

	@Inject
	private DependencyGraphBuilder dependencyGraphBuilder;

	private List<String> excludes = new ArrayList<>();

	private String includedScopes = "compile,provided,runtime,test,system,import";

	private DependencyNode getNode() throws EnforcerRuleException
	{
		try
		{
			ProjectBuildingRequest buildingRequest =
				new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject(project);
			DependencyNode node = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, null);
			return node;
		}
		catch (DependencyGraphBuilderException e)
		{
			throw new EnforcerRuleException(
				"Could not build dependency graph " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void execute() throws EnforcerRuleException
	{
		try
		{
			getLog().debug("RequireManagedDependencies excluding " + excludes);
			getLog().debug("RequireManagedDependencies with scopes " + includedScopes);
			DependencyNode node = getNode();
			UnmanagedDependencyCollector visitor =
				new UnmanagedDependencyCollector(project, node,
					excludes, Arrays.asList(includedScopes.split(",")));
			if (!node.accept(visitor))
			{
				throw visitor.getStoredException();
			}

			boolean fail = false;
			for (CharSequence unmanaged : visitor.getUnmanagedDependencies())
			{
				fail = true;
				getLog().warn("Unmanaged dependency found: " + unmanaged);
			}
			if (fail)
			{
				throw new EnforcerRuleException(
					"Failed while enforcing releasability. See above detailed error message.");
			}
		}
		catch (Exception e)
		{
			throw new EnforcerRuleException(e.getLocalizedMessage(), e);
		}
	}
}
