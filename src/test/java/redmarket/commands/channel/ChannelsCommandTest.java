package redmarket.commands.channel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.channel.ServerVoiceChannelBuilder;
import org.javacord.api.entity.permission.Permissions;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bbaker.discord.redmarket.commands.channel.ChannelCommand;
import com.bbaker.discord.redmarket.db.DatabaseService;

import redmarket.CommonMocks;

class ChannelsCommandTest extends CommonMocks {


	private DatabaseService dbService = null;
	private DiscordApi api = null;
	private ChannelCategory channelCategory = null;
	private Role gmRole = null;

	private ChannelCommand cmd = null;

	private static final String CATEGORY = "test";


	@BeforeEach
    public void setup() {
		dbService = mock(DatabaseService.class);
		api = mock(DiscordApi.class);
		channelCategory = mock(ChannelCategory.class);
		gmRole = mock(Role.class);
		when(gmRole.getName()).thenReturn("gm");
		when(api.getChannelCategoriesByNameIgnoreCase(CATEGORY)).thenReturn(Arrays.asList(channelCategory));



		cmd = new ChannelCommand(api, CATEGORY, dbService);

	}

	@Test
	void testCreateChannel() {
		ServerTextChannelBuilder stcBuilder = mock(ServerTextChannelBuilder.class);
		when(stcBuilder.setName("hello-world")).thenReturn(stcBuilder);
		when(stcBuilder.setCategory(channelCategory)).thenReturn(stcBuilder);
		when(stcBuilder.addPermissionOverwrite(any(Role.class), any())).thenReturn(stcBuilder);
		when(stcBuilder.addPermissionOverwrite(any(User.class), any())).thenReturn(stcBuilder);

		ServerVoiceChannelBuilder svcBuilder = mock(ServerVoiceChannelBuilder.class);
		when(svcBuilder.setName("hello-world")).thenReturn(svcBuilder);
		when(svcBuilder.setCategory(channelCategory)).thenReturn(svcBuilder);
		when(svcBuilder.addPermissionOverwrite(any(Role.class), any())).thenReturn(svcBuilder);
		when(svcBuilder.addPermissionOverwrite(any(User.class), any())).thenReturn(svcBuilder);


		when(channelCategory.getOverwrittenPermissions(any(Role.class))).thenReturn(mock(Permissions.class));

		when(USER.getRoles(SERVER)).thenReturn(Arrays.asList(gmRole));
		when(SERVER.getEveryoneRole()).thenReturn(mock(Role.class));
		when(SERVER.createTextChannelBuilder()).thenReturn(stcBuilder);



		String actual = cmd.onChannel(genMsg("!c create hello-world"));

		assertEquals(String.format(cmd.MSG_CHANNEL_CREATED, "hello-world"), actual, "Display the success message for the text and voice channel to be created");
	}


}
