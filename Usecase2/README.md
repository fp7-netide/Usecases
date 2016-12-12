# Use Case 2 : Integrated IT System

## Use Case Presentation

### Overview

This use case describes an Integrated System (IS) which will typically be deployed within a larger data centre. Looked at from the outside, from an infrastructure management perspective, the IS behaves as one computer with high processing capabilities. Looked at from the inside the IS is a network of closely collaborating computers. The Use Case addresses the customer need to rapidly introduce highly scalable computing infrastructure within an existing data centre environment. The solution is encapsulated from sensitive network management processes in the rest of the data centre and can therefore be deployed without causing network management conflicts. Most existing server solutions expose more of their network management to the surrounding environment. It is hard to rapidly integrate many of them without disrupting the existing environment. The solution has its own internal network which has to be defined, tested, deployed and maintained. The solution benefits from the clear separation of the control and forwarding planes which underlies SDN.

### Illustration

![Alt text](usecase2.png?raw=true " ")

### Installation

Currently, there are two versions of the UC implemented: one implemented in Pyretic for the first release of the NetIDE Engine (which followed the Pyretic intermediate protocol) and a second one implemented in Ryu for the second release of the NetIDE Engine (which follows the NetIDE intermediate protocol).

Check each folder (```pyretic```/```ryu```) for each specific implementation and installation steps.

The ```NetIDE``` folder contains the composition files required by the NetIDE Core when using the Engine.

### Testing

This use case has been installed and experimented with using v1.0 of the NetIDE software. A particular focus was on the usability of the installation process, the ease of use of the topology editor and the tools interfaces provided by the IDE. The ```examples/UC2``` folder in the IDE repository contains the full NetIDE project of UC2 (to be directly loaded in the IDE).
