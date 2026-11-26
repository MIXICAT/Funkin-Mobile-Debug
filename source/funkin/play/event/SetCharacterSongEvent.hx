package funkin.play.event;

import funkin.data.character.CharacterData.CharacterDataParser;
import funkin.data.event.SongEventSchema;
import funkin.data.event.SongEventSchema.SongEventFieldType;
import funkin.data.song.SongData.SongEventData;
import funkin.play.character.BaseCharacter;
import funkin.play.character.BaseCharacter.CharacterType;

/**
 * Replaces one of the stage's character slots with another character.
 *
 * The target slot's stage position, scale, camera offsets, and behavior are
 * reapplied by Stage.addCharacter().
 */
class SetCharacterSongEvent extends SongEvent
{
  static final DEFAULT_TARGET:String = 'bf';
  static final DEFAULT_CHARACTER:String = '';

  public function new()
  {
    super('SetCharacter');
  }

  override public function handleEvent(data:SongEventData):Void
  {
    if (PlayState.instance == null || PlayState.instance.currentStage == null) return;

    var stage = PlayState.instance.currentStage;
    var target:String = data.getString('target') ?? DEFAULT_TARGET;
    var characterId:String = data.getString('character') ?? DEFAULT_CHARACTER;

    if (characterId == '')
    {
      trace('SetCharacter: No character ID was provided.');
      return;
    }

    var oldCharacter:Null<BaseCharacter> = null;
    var characterType:CharacterType;

    // Resolve the slot first. The event can also populate an empty GF slot.
    switch (target)
    {
      case 'bf':
        oldCharacter = stage.getBoyfriend();
        characterType = CharacterType.BF;
      case 'dad':
        oldCharacter = stage.getDad();
        characterType = CharacterType.DAD;
      case 'gf':
        oldCharacter = stage.getGirlfriend();
        characterType = CharacterType.GF;
      default:
        trace('SetCharacter: Unknown target slot: ' + target);
        return;
    }

    if (oldCharacter != null && oldCharacter.characterId == characterId) return;

    // Load the replacement before removing the current character. A bad ID
    // therefore leaves the existing character untouched.
    var newCharacter:Null<BaseCharacter> = CharacterDataParser.fetchCharacter(characterId);
    if (newCharacter == null)
    {
      trace('SetCharacter: Could not load character ID: ' + characterId);
      return;
    }

    // Remove the old sprite and its slot mapping when the slot is occupied.
    if (oldCharacter != null)
    {
      switch (target)
      {
        case 'bf':
          stage.getBoyfriend(true);
        case 'dad':
          stage.getDad(true);
        case 'gf':
          stage.getGirlfriend(true);
      }

      oldCharacter.characterType = CharacterType.OTHER;
      oldCharacter.destroy();
    }

    // Stage.addCharacter reapplies the correct slot position, scale,
    // z-index, camera offsets, health icon, and character behavior.
    stage.addCharacter(newCharacter, characterType);
    stage.refresh();

    trace('SetCharacter: Replaced ' + target + ' with ' + characterId);
  }

  override public function getTitle():String
  {
    return 'Character Swap';
  }

  override public function getEventSchema():SongEventSchema
  {
    return new SongEventSchema([
      {
        name: 'target',
        title: 'Character Slot',
        type: SongEventFieldType.ENUM,
        keys: [
          'Player (BF)' => 'bf',
          'Opponent (Dad)' => 'dad',
          'Girlfriend (GF)' => 'gf'
        ],
        defaultValue: DEFAULT_TARGET
      },
      {
        name: 'character',
        title: 'New Character ID',
        type: SongEventFieldType.STRING,
        defaultValue: DEFAULT_CHARACTER
      }
    ]);
  }
}
