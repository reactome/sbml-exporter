
# TODO #

These are TODO markers in the code that could not be done by may be possible to address in the future:

## TODO: what if there is more than one compartment listed

**WriteSBML**

function: addSpecies

line: 598

The PhysicalEntity function getCompartment() returns a List<Compartment> objects. The issue of whether an entity can exist in more than one compartment has been raised and is being considered. At present the SBMLExporter code merely takes the first entry in this list as the compartment in which the entity is located.
 
## TODO: Must be a better way to do this

**WriteSBML**

function: determineParentPathway

line: 318

The WriteSBML class was written to facilitate the possibility of allowing a user to construct their own model from events within pathways. This is not yet in use. The determineParentPathway function attempts to identify if the List<Event> being used to construct the model have a unique parent pathway that could be referenced.

I'm sure the code doing this is not particularly optimal and if this functionality were to be utilised it should be reconsidered.


-----
This file was last updated in May 2018.
